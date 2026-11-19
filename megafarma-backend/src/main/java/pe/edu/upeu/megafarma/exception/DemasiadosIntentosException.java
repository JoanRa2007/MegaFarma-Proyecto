package pe.edu.upeu.megafarma.exception;

/*
 * PARA ESTUDIAR · MANEJO DE ERRORES (exception)
 * Demasiados inicios de sesion fallidos desde la misma IP. Se traduce a
 * HTTP 429 Too Many Requests con la cabecera Retry-After.
 */
public class DemasiadosIntentosException extends RuntimeException {

    private final long segundosParaReintentar;

    public DemasiadosIntentosException(String mensaje, long segundosParaReintentar) {
        super(mensaje);
        this.segundosParaReintentar = segundosParaReintentar;
    }

    public long getSegundosParaReintentar() {
        return segundosParaReintentar;
    }
}
