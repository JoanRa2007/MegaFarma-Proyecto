package pe.edu.upeu.megafarma.service.impl;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - IMPLEMENTACION (service.impl)
 * Inicio y cierre de sesion con varias capas de proteccion:
 *  1. Limite de fallos por IP (fuerza bruta) -> HTTP 429.
 *  2. Contrasenas con BCrypt (Spring Security compara el hash).
 *  3. Bloqueo de la cuenta tras 5 fallos seguidos (HU-46).
 *  4. Mensaje unico "Usuario o contrasena incorrectos" (no revela cual fallo).
 *  5. Todo intento queda en la bitacora de auditoria.
 *  6. Logout revoca el token (su jti) aunque no haya expirado.
 */

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import pe.edu.upeu.megafarma.dto.auth.LoginRequestDTO;
import pe.edu.upeu.megafarma.dto.auth.LoginResponseDTO;
import pe.edu.upeu.megafarma.dto.auth.UsuarioResponseDTO;
import pe.edu.upeu.megafarma.entity.Usuario;
import pe.edu.upeu.megafarma.exception.DemasiadosIntentosException;
import pe.edu.upeu.megafarma.mapper.UsuarioMapper;
import pe.edu.upeu.megafarma.repository.UsuarioRepository;
import pe.edu.upeu.megafarma.security.JwtService;
import pe.edu.upeu.megafarma.security.LimitadorIntentosLogin;
import pe.edu.upeu.megafarma.security.PermisosPorRol;
import pe.edu.upeu.megafarma.security.TokensRevocados;
import pe.edu.upeu.megafarma.security.UsuarioActual;
import pe.edu.upeu.megafarma.service.AuditoriaService;
import pe.edu.upeu.megafarma.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final UsuarioActual usuarioActual;
    private final LimitadorIntentosLogin limitador;
    private final TokensRevocados tokensRevocados;
    private final AuditoriaService auditoria;
    private final int maxIntentosFallidos;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UsuarioRepository usuarioRepository,
                           JwtService jwtService,
                           UsuarioActual usuarioActual,
                           LimitadorIntentosLogin limitador,
                           TokensRevocados tokensRevocados,
                           AuditoriaService auditoria,
                           @Value("${app.seguridad.max-intentos-fallidos:5}") int maxIntentosFallidos) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.usuarioActual = usuarioActual;
        this.limitador = limitador;
        this.tokensRevocados = tokensRevocados;
        this.auditoria = auditoria;
        this.maxIntentosFallidos = maxIntentosFallidos;
    }

    /*
     * HU-21 Iniciar sesion. No lleva @Transactional a proposito: el contador de
     * intentos y la auditoria deben guardarse aunque el metodo lance la excepcion.
     */
    @Override
    public LoginResponseDTO login(LoginRequestDTO request, String ip) {

        String username = request.getUsername().trim().toLowerCase();

        if (limitador.estaBloqueada(ip)) {
            long segundos = limitador.segundosParaReintentar(ip);
            auditoria.registrarDe(username, "LIMITE_INTENTOS", "Demasiados intentos fallidos desde " + ip, false);
            throw new DemasiadosIntentosException(
                    "Demasiados intentos fallidos. Espere " + segundos + " segundos e intente de nuevo.", segundos);
        }

        try {
            Authentication autenticacion = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword()));

            UserDetails principal = (UserDetails) autenticacion.getPrincipal();
            Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();

            if (usuario.getIntentosFallidos() > 0) {
                usuario.setIntentosFallidos(0);
                usuarioRepository.save(usuario);
            }

            String rol = usuario.getRol().getNombre();
            auditoria.registrarDe(username, "LOGIN_OK", "Rol " + rol, true);

            return new LoginResponseDTO(
                    jwtService.generarToken(principal),
                    "Bearer",
                    jwtService.getExpiracionSegundos(),
                    usuario.getUsername(),
                    usuario.getNombreCompleto(),
                    rol,
                    PermisosPorRol.permisos(rol));

        } catch (BadCredentialsException ex) {
            limitador.registrarFallo(ip);
            usuarioRepository.findByUsername(username).ifPresentOrElse(usuario -> {
                usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
                usuarioRepository.save(usuario);
                if (usuario.getIntentosFallidos() >= maxIntentosFallidos) {
                    auditoria.registrarDe(username, "CUENTA_BLOQUEADA",
                            "Bloqueada tras " + usuario.getIntentosFallidos() + " intentos fallidos", false);
                } else {
                    auditoria.registrarDe(username, "LOGIN_FALLIDO", "Contraseña incorrecta", false);
                }
            }, () -> auditoria.registrarDe(username, "LOGIN_FALLIDO", "Usuario inexistente", false));
            throw ex;
        } catch (AuthenticationException ex) {
            // Cuenta bloqueada o desactivada: tambien cuenta como fallo de la IP.
            limitador.registrarFallo(ip);
            auditoria.registrarDe(username, "LOGIN_RECHAZADO", ex.getClass().getSimpleName(), false);
            throw ex;
        }
    }

    @Override
    public void logout(String token) {
        Claims claims = jwtService.leerClaims(token);
        tokensRevocados.revocar(claims.getId(), claims.getExpiration().toInstant());
        auditoria.registrar("LOGOUT", "Token revocado", true);
    }

    @Override
    public UsuarioResponseDTO perfil() {
        return UsuarioMapper.toResponse(usuarioActual.obtener(), maxIntentosFallidos);
    }
}
