package pe.edu.upeu.megafarma.security;

/*
 * PARA ESTUDIAR · SEGURIDAD (security)
 * Usuario autenticado sin el permiso que exige la ruta -> HTTP 403 en JSON.
 * Cada acceso denegado queda en la bitacora de auditoria.
 */

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import pe.edu.upeu.megafarma.service.AuditoriaService;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final AuditoriaService auditoriaService;

    public RestAccessDeniedHandler(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        auditoriaService.registrar("ACCESO_DENEGADO", request.getMethod() + " " + request.getRequestURI(), false);
        RespuestaErrorSeguridad.escribir(response, HttpStatus.FORBIDDEN,
                "No tiene permisos para realizar esta operación",
                request.getRequestURI());
    }
}
