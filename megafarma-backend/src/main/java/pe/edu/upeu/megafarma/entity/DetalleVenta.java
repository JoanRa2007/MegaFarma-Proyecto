package pe.edu.upeu.megafarma.entity;

/*
 * PARA ESTUDIAR · CAPA ENTIDAD (entity)
 * Cada entidad es una tabla de la base de datos y cada atributo es una columna.
 * JPA/Hibernate convierte filas en objetos Java y objetos en filas.
 * El nombre de la columna sale del atributo: precioVenta -> precio_venta.
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;

// @Entity: esta clase es una entidad JPA: representa una tabla
@Entity
// @Table: nombre exacto de la tabla en la base de datos
@Table(name = "detalle_ventas")
// @Getter: Lombok genera los metodos getX() de todos los atributos al compilar
@Getter
// @Setter: Lombok genera los metodos setX(valor) de todos los atributos
@Setter
// @NoArgsConstructor: Lombok genera el constructor vacio (lo necesitan JPA y el conversor JSON)
@NoArgsConstructor
public class DetalleVenta {

    // @Id: este atributo es la clave primaria
    @Id
    // @GeneratedValue: el id lo genera la base (columna IDENTITY / autoincremental)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToOne: relacion muchos-a-uno (llave foranea). LAZY = el otro objeto se carga solo si se usa
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // @JoinColumn: nombre de la columna que guarda la llave foranea
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    /* Lote del que salio la mercaderia (trazabilidad y anulacion). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    private int cantidad;

    // @Column: ajusta la columna: precision/scale para decimales, length para textos
    @Column(precision = 10, scale = 2)
    // BigDecimal: decimales exactos para dinero (double pierde centimos por redondeo)
    private BigDecimal precioUnitario;

    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal;
}
