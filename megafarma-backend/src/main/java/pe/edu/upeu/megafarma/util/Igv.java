package pe.edu.upeu.megafarma.util;

/*
 * PARA ESTUDIAR · UTILIDADES (util)
 * Desglose del IGV (18 %). En la botica los precios de venta ya incluyen
 * el impuesto: de un importe de S/ 11.80 la base (valor de venta u
 * operacion gravada) es S/ 10.00 y el IGV es S/ 1.80.
 */

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Igv {

    public static final BigDecimal TASA = new BigDecimal("0.18");
    private static final BigDecimal FACTOR = BigDecimal.ONE.add(TASA);

    private Igv() {
    }

    /* Valor sin IGV de un importe que incluye IGV. */
    public static BigDecimal valorSinIgv(BigDecimal importeConIgv) {
        return importeConIgv.divide(FACTOR, 2, RoundingMode.HALF_UP);
    }

    /* IGV contenido en un importe: importe - valor sin IGV (asi la suma cuadra al centimo). */
    public static BigDecimal igvDe(BigDecimal importeConIgv) {
        return importeConIgv.setScale(2, RoundingMode.HALF_UP).subtract(valorSinIgv(importeConIgv));
    }
}
