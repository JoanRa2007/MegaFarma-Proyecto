package pe.edu.upeu.megafarma.enums;

/* Urgencia de un lote segun los dias que faltan para su vencimiento. */
public enum NivelAlerta {
    /* Vencido o vence dentro del umbral critico (30 dias por defecto). */
    CRITICO,
    /* Vence dentro del umbral medio (60 dias por defecto). */
    MEDIO,
    /* Vence dentro de la ventana de alerta (90 dias por defecto). */
    BAJO
}
