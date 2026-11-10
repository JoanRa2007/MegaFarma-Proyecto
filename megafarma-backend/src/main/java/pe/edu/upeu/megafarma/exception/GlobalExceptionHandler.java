package pe.edu.upeu.megafarma.exception;

/*
 * PARA ESTUDIAR · MANEJO DE ERRORES (exception)
 * Excepciones propias del negocio y la clase que las convierte en respuestas
 * HTTP con un formato JSON unico.
 */

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pe.edu.upeu.megafarma.exception.dto.ErrorResponseDTO;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// @RestControllerAdvice: intercepta las excepciones de todos los controladores
@RestControllerAdvice
public class GlobalExceptionHandler {

    // logger: escribe mensajes en la consola/archivo de log de la aplicacion
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /* Recurso no encontrado -> HTTP 404 */
    // @ExceptionHandler: este metodo atiende la excepcion indicada y arma la respuesta de error
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(
            RecursoNoEncontradoException ex, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    /* Regla de negocio -> HTTP 409 */
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinessRule(
            ReglaNegocioException ex, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    /* Errores de @Valid -> HTTP 400 con el detalle por campo */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.putIfAbsent(e.getField(), e.getDefaultMessage()));

        return responder(HttpStatus.BAD_REQUEST, "Existen errores de validación", request, errores);
    }

    /* Parametro con formato invalido (fecha mal formada, id no numerico) -> HTTP 400 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        Map<String, String> errores = new LinkedHashMap<>();
        errores.put(ex.getName(), "Valor inválido: '" + ex.getValue() + "'");

        return responder(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un formato inválido", request, errores);
    }

    /* Cuerpo JSON ilegible (sintaxis, enum inexistente, tipo equivocado) -> HTTP 400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición no es válido o está mal formado", request, null);
    }

    /*
     * Violacion de integridad en la base (unique, FK, check). Salta cuando
     * dos peticiones concurrentes pasan la validacion del servicio y la
     * restriccion de la tabla frena la segunda. No se expone el mensaje
     * del driver. -> HTTP 409
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.warn("Violación de integridad en {} | causa={}",
                request.getRequestURI(), ex.getMostSpecificCause().getMessage());

        return responder(HttpStatus.CONFLICT,
                "La operación viola una restricción de integridad de datos "
                        + "(registro duplicado o referenciado por otros)", request, null);
    }

    /*
     * Credenciales invalidas, usuario desactivado o bloqueado en el login.
     * El mensaje es el mismo en todos los casos para no revelar cual fallo.
     * -> HTTP 401
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {

        log.warn("Autenticación fallida en {} | causa={}", request.getRequestURI(), ex.getMessage());

        return responder(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos", request, null);
    }

    /* Usuario autenticado sin el permiso necesario -> HTTP 403 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return responder(HttpStatus.FORBIDDEN,
                "No tiene permisos para realizar esta operación", request, null);
    }

    /*
     * Cualquier otra excepcion. Las propias de Spring MVC (ruta inexistente
     * 404, metodo no soportado 405, parametro ausente 400) traen su codigo:
     * se respeta y solo se cambia el formato. El resto se registra completo
     * en el log y al cliente le llega un mensaje generico con HTTP 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleUnexpected(
            Exception ex, HttpServletRequest request) {

        if (ex instanceof ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            HttpStatus resolved = HttpStatus.resolve(status.value());
            ErrorResponseDTO error = new ErrorResponseDTO(
                    LocalDateTime.now(),
                    status.value(),
                    resolved == null ? "Error" : resolved.getReasonPhrase(),
                    springError.getBody().getDetail(),
                    request.getRequestURI(),
                    null);
            return ResponseEntity.status(status).body(error);
        }

        log.error("Error no controlado en {}", request.getRequestURI(), ex);

        return responder(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Intente nuevamente más tarde.", request, null);
    }

    private ResponseEntity<ErrorResponseDTO> responder(
            HttpStatus status, String mensaje, HttpServletRequest request,
            Map<String, String> erroresValidacion) {

        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                request.getRequestURI(),
                erroresValidacion);

        return ResponseEntity.status(status).body(error);
    }
}
