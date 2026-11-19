package pe.edu.upeu.megafarma.exception;

/*
 * PARA ESTUDIAR · MANEJO DE ERRORES (exception)
 * La solicitud tiene datos invalidos que no detecta @Valid (por ejemplo un
 * rango de fechas invertido). Se traduce a HTTP 400. Sprint 2 · CP-S2-48.
 */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
