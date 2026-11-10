package pe.edu.upeu.megafarma.security;

/*
 * PARA ESTUDIAR · SEGURIDAD (security)
 * Clases de autenticacion (quien eres: token JWT) y autorizacion (que puedes
 * hacer: permisos del rol).
 */

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/* Peticion sin token, con token vencido o invalido -> HTTP 401 en JSON. */
// @Component: Spring crea un unico objeto de esta clase y lo inyecta donde se necesite
@Component
// implements: esta clase cumple el contrato (los metodos) de esa interfaz
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // @Override: este metodo implementa o redefine uno declarado en la interfaz o clase padre
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        RespuestaErrorSeguridad.escribir(response, HttpStatus.UNAUTHORIZED,
                "Debe iniciar sesión: token ausente, inválido o expirado",
                request.getRequestURI());
    }
}
