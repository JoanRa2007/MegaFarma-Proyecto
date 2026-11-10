package pe.edu.upeu.megafarma.security;

/*
 * PARA ESTUDIAR · SEGURIDAD (security)
 * Clases de autenticacion (quien eres: token JWT) y autorizacion (que puedes
 * hacer: permisos del rol).
 */

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/* Usuario autenticado sin el permiso que exige la ruta -> HTTP 403 en JSON. */
// @Component: Spring crea un unico objeto de esta clase y lo inyecta donde se necesite
@Component
// implements: esta clase cumple el contrato (los metodos) de esa interfaz
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    // @Override: este metodo implementa o redefine uno declarado en la interfaz o clase padre
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        RespuestaErrorSeguridad.escribir(response, HttpStatus.FORBIDDEN,
                "No tiene permisos para realizar esta operación",
                request.getRequestURI());
    }
}
