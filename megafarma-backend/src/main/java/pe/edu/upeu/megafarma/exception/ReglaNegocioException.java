package pe.edu.upeu.megafarma.exception;

/*
 * PARA ESTUDIAR · MANEJO DE ERRORES (exception)
 * Excepciones propias del negocio y la clase que las convierte en respuestas
 * HTTP con un formato JSON unico.
 */

/* Una regla del negocio impide la operacion. Se traduce a HTTP 409. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
