package pe.edu.upeu.megafarma.entity;

/*
 * PARA ESTUDIAR · CAPA ENTIDAD (entity)
 * Comprobante electronico (boleta o factura) de una venta. Guarda los datos
 * del cliente, el desglose del IGV, el XML en formato UBL 2.1 y su hash.
 * Modo demostracion: se genera igual que uno real, pero no se envia a SUNAT.
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.megafarma.enums.TipoComprobante;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "comprobantes")
@Getter
@Setter
@NoArgsConstructor
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private TipoComprobante tipo;

    private String serie;

    private int numero;

    private String clienteTipoDoc;

    private String clienteNumDoc;

    private String clienteNombre;

    @Column(precision = 12, scale = 2)
    private BigDecimal opGravada;

    @Column(precision = 12, scale = 2)
    private BigDecimal igv;

    @Column(precision = 12, scale = 2)
    private BigDecimal total;

    private LocalDateTime fechaEmision;

    /* ACEPTADO_DEMO al emitir; ANULADO si la venta se anula. */
    private String estado;

    /* Huella SHA-256 del XML (en SUNAT seria el resumen de la firma digital). */
    private String hash;

    @Column(length = 20000)
    private String xml;

    /* B001-00000001 */
    public String numeroCompleto() {
        return serie + "-" + String.format("%08d", numero);
    }
}
