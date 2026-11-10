package pe.edu.upeu.megafarma.security;

/*
 * PARA ESTUDIAR · SEGURIDAD (security)
 * Clases de autenticacion (quien eres: token JWT) y autorizacion (que puedes
 * hacer: permisos del rol).
 */

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/*
 * Los errores 401/403 nacen en la cadena de filtros, antes de llegar a los
 * controladores, asi que GlobalExceptionHandler no los ve. Esta clase
 * escribe el mismo formato JSON de ErrorResponseDTO.
 */
final class RespuestaErrorSeguridad {

    private RespuestaErrorSeguridad() {
    }

    static void escribir(HttpServletResponse response, HttpStatus status,
                         String mensaje, String ruta) throws IOException {

        String json = "{\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":" + status.value() + ","
                + "\"error\":\"" + status.getReasonPhrase() + "\","
                + "\"message\":\"" + escapar(mensaje) + "\","
                + "\"path\":\"" + escapar(ruta) + "\","
                + "\"validationErrors\":null}";

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json);
    }

    private static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(texto.length());
        for (char c : texto.toCharArray()) {
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c < 0x20) {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
