package pe.edu.upeu.megafarma.enums;

/* Comprobantes de pago electronicos de SUNAT que emite la botica. */
public enum TipoComprobante {
    /* Boleta de venta (codigo SUNAT 03): consumidor final. */
    BOLETA("03", "B001"),
    /* Factura (codigo SUNAT 01): cliente con RUC. */
    FACTURA("01", "F001");

    private final String codigoSunat;
    private final String serie;

    TipoComprobante(String codigoSunat, String serie) {
        this.codigoSunat = codigoSunat;
        this.serie = serie;
    }

    public String getCodigoSunat() {
        return codigoSunat;
    }

    public String getSerie() {
        return serie;
    }
}
