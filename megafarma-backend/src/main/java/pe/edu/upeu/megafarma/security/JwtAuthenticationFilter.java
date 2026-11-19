package pe.edu.upeu.megafarma.security;

/*
 * PARA ESTUDIAR · SEGURIDAD (security)
 * Lee "Authorization: Bearer <token>" en cada peticion. El token se acepta solo si:
 *  - la firma y el emisor son validos y no expiro,
 *  - no fue revocado con un logout,
 *  - el usuario sigue activo y sin bloqueo (se consulta la base en cada peticion).
 * Si algo falla, la peticion sigue como anonima y Spring Security responde 401.
 *
 * No es un @Component: se crea en SecurityConfig para que Spring Boot no
 * lo registre dos veces (como filtro del servlet y dentro de la cadena).
 */

import io.jsonwebtoken.Claims;
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

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokensRevocados tokensRevocados;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService,
                                   TokensRevocados tokensRevocados) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.tokensRevocados = tokensRevocados;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String cabecera = request.getHeader("Authorization");

        if (cabecera != null && cabecera.startsWith(PREFIJO)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = cabecera.substring(PREFIJO.length()).trim();

            try {
                Claims claims = jwtService.leerClaims(token);

                if (!tokensRevocados.estaRevocado(claims.getId())) {
                    UserDetails usuario = userDetailsService.loadUserByUsername(claims.getSubject());

                    if (usuario.isEnabled() && usuario.isAccountNonLocked()) {
                        UsernamePasswordAuthenticationToken autenticacion =
                                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                        autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(autenticacion);
                    }
                }
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
