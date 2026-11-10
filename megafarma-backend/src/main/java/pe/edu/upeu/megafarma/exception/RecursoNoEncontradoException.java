package pe.edu.upeu.megafarma.exception;

/*
 * PARA ESTUDIAR · MANEJO DE ERRORES (exception)
 * Excepciones propias del negocio y la clase que las convierte en respuestas
 * HTTP con un formato JSON unico.
 */

/* El recurso pedido no existe. Se traduce a HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
