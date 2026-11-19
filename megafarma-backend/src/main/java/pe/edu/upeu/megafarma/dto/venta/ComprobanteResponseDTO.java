package pe.edu.upeu.megafarma.dto.venta;

/*
 * PARA ESTUDIAR · DTO (Data Transfer Object)
 * Comprobante electronico listo para mostrarse o imprimirse como ticket.
 */

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComprobanteResponseDTO {
    private String tipo;
    private String codigoSunat;
    private String numero;
    private LocalDateTime fechaEmision;
    private String estado;
    // Emisor
    private String emisorRuc;
    private String emisorRazonSocial;
    private String emisorDireccion;
    // Cliente
    private String clienteTipoDoc;
    private String clienteNumDoc;
    private String clienteNombre;
    // Importes
    private List<Item> items;
    private BigDecimal opGravada;
    private BigDecimal igv;
    private BigDecimal total;
    private String totalEnLetras;
    private List<PagoResponseDTO> pagos;
    private BigDecimal montoRecibido;
    private BigDecimal vuelto;
    // Validacion
    private String hash;
    private String textoQr;
    private String ventaNumero;
    private boolean modoDemostracion;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String codigo;
        private String descripcion;
        private int cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal valorVenta;
        private BigDecimal igv;
        private BigDecimal importe;
    }
}
