package pe.edu.upeu.megafarma.service.impl;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - IMPLEMENTACION (service.impl)
 * Aqui viven las reglas de negocio. El controlador llama al servicio, el servicio
 * valida las reglas, usa los repositorios y devuelve un DTO de respuesta.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import pe.edu.upeu.megafarma.dto.auth.LoginRequestDTO;
import pe.edu.upeu.megafarma.dto.auth.LoginResponseDTO;
import pe.edu.upeu.megafarma.dto.auth.UsuarioResponseDTO;
import pe.edu.upeu.megafarma.entity.Usuario;
import pe.edu.upeu.megafarma.mapper.UsuarioMapper;
import pe.edu.upeu.megafarma.repository.UsuarioRepository;
import pe.edu.upeu.megafarma.security.JwtService;
import pe.edu.upeu.megafarma.security.PermisosPorRol;
import pe.edu.upeu.megafarma.security.UsuarioActual;
import pe.edu.upeu.megafarma.service.AuthService;

// @Service: Spring crea un unico objeto de esta clase y lo inyecta donde se necesite
@Service
// implements: esta clase cumple el contrato (los metodos) de esa interfaz
public class AuthServiceImpl implements AuthService {

    // logger: escribe mensajes en la consola/archivo de log de la aplicacion
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final UsuarioActual usuarioActual;
    private final int maxIntentosFallidos;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UsuarioRepository usuarioRepository,
                           JwtService jwtService,
                           UsuarioActual usuarioActual,
                           // @Value: inyecta un valor de application.yaml (lo que va tras ':' es el valor por defecto)
                           @Value("${app.seguridad.max-intentos-fallidos:5}") int maxIntentosFallidos) {
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.usuarioActual = usuarioActual;
        this.maxIntentosFallidos = maxIntentosFallidos;
    }

    /*
     * HU-21 Iniciar sesion. Spring Security compara la contrasena con el
     * hash BCrypt. Si es correcta se devuelve un JWT; si falla se cuenta el
     * intento (HU-46) y la cuenta se bloquea al llegar al maximo.
     *
     * No lleva @Transactional a proposito: el contador de intentos debe
     * guardarse aunque el metodo termine lanzando la excepcion.
     */
    // @Override: este metodo implementa o redefine uno declarado en la interfaz o clase padre
    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {

        String username = request.getUsername().trim().toLowerCase();

        try {
            Authentication autenticacion = authenticationManager.authenticate(
                    // objeto con el usuario (y su clave o sus permisos) que entiende Spring Security
                    new UsernamePasswordAuthenticationToken(username, request.getPassword()));

            UserDetails principal = (UserDetails) autenticacion.getPrincipal();
            // findBy... devuelve un Optional: si viene vacio se lanza la excepcion indicada
            Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();

            if (usuario.getIntentosFallidos() > 0) {
                usuario.setIntentosFallidos(0);
                usuarioRepository.save(usuario);
            }

            String rol = usuario.getRol().getNombre();
            // deja registro de la operacion en el log
            log.info("Inicio de sesión correcto | usuario={} rol={}", username, rol);

            return new LoginResponseDTO(
                    jwtService.generarToken(principal),
                    "Bearer",
                    jwtService.getExpiracionSegundos(),
                    usuario.getUsername(),
                    usuario.getNombreCompleto(),
                    rol,
                    PermisosPorRol.permisos(rol));

        } catch (BadCredentialsException ex) {
            usuarioRepository.findByUsername(username).ifPresent(usuario -> {
                usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
                usuarioRepository.save(usuario);
                if (usuario.getIntentosFallidos() >= maxIntentosFallidos) {
                    log.warn("Cuenta bloqueada por intentos fallidos | usuario={}", username);
                }
            });
            throw ex;
        }
    }

    @Override
    public UsuarioResponseDTO perfil() {
        return UsuarioMapper.toResponse(usuarioActual.obtener(), maxIntentosFallidos);
    }
}
