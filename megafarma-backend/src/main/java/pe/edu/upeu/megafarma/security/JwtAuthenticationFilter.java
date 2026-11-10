package pe.edu.upeu.megafarma.security;

/*
 * PARA ESTUDIAR · SEGURIDAD (security)
 * Clases de autenticacion (quien eres: token JWT) y autorizacion (que puedes
 * hacer: permisos del rol).
 */

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/*
 * Lee "Authorization: Bearer <token>" en cada peticion. Si el token es
 * valido y el usuario sigue activo, deja la autenticacion en el contexto.
 * Si no, la peticion sigue como anonima y Spring Security responde 401.
 *
 * No es un @Component: se crea en SecurityConfig para que Spring Boot no
 * lo registre dos veces (como filtro del servlet y dentro de la cadena).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // constante de la clase: un solo valor compartido que no cambia
    private static final String PREFIJO = "Bearer ";

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    // @Override: este metodo implementa o redefine uno declarado en la interfaz o clase padre
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String cabecera = request.getHeader("Authorization");

        if (cabecera != null && cabecera.startsWith(PREFIJO)
                // SecurityContextHolder: guarda quien es el usuario autenticado durante la peticion
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = cabecera.substring(PREFIJO.length()).trim();

            try {
                String username = jwtService.extraerUsername(token);

                // Se vuelve a consultar la base: un usuario desactivado o
                // bloqueado deja de tener acceso aunque su token no haya expirado.
                UserDetails usuario = userDetailsService.loadUserByUsername(username);

                if (usuario.isEnabled() && usuario.isAccountNonLocked()) {
                    UsernamePasswordAuthenticationToken autenticacion =
                            // objeto con el usuario (y su clave o sus permisos) que entiende Spring Security
                            new UsernamePasswordAuthenticationToken(
                                    usuario, null, usuario.getAuthorities());
                    autenticacion.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(autenticacion);
                }
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
