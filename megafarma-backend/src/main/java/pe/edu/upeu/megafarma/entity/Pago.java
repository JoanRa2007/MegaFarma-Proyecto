package pe.edu.upeu.megafarma.entity;

/*
 * PARA ESTUDIAR · CAPA ENTIDAD (entity)
 * Una forma de pago usada en una venta. Una venta puede tener varias
 * (pago mixto): por ejemplo S/ 20 en efectivo y S/ 15.50 con Yape.
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.megafarma.enums.MedioPago;

import java.math.BigDecimal;

@Entity
@Table(name = "pagos")
@Getter
@Setter
@NoArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private MedioPago medio;

    /* Monto aplicado a la venta (en efectivo, sin contar el vuelto). */
    @Column(precision = 12, scale = 2)
    private BigDecimal monto;

    /* Numero de operacion (Yape, Plin, transferencia) o ultimos 4 digitos de la tarjeta. */
    private String referencia;
}
