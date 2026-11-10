package pe.edu.upeu.megafarma.enums;

/*
 * PARA ESTUDIAR · ENUM
 * Lista cerrada de valores posibles. En la base se guarda el nombre como texto.
 */

/* Tipos de movimiento del kardex. */
// enum: tipo con una lista fija de valores posibles
public enum TipoMovimiento {
    ENTRADA,
    SALIDA_VENTA,
    ANULACION_VENTA,
    AJUSTE_POSITIVO,
    AJUSTE_NEGATIVO,
    BAJA_VENCIMIENTO
}
