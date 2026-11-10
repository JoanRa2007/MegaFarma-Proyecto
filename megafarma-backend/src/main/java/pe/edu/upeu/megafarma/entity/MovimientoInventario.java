package pe.edu.upeu.megafarma.entity;

/*
 * PARA ESTUDIAR · CAPA ENTIDAD (entity)
 * Cada entidad es una tabla de la base de datos y cada atributo es una columna.
 * JPA/Hibernate convierte filas en objetos Java y objetos en filas.
 * El nombre de la columna sale del atributo: precioVenta -> precio_venta.
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
import pe.edu.upeu.megafarma.enums.TipoMovimiento;

import java.time.LocalDateTime;

// @Entity: esta clase es una entidad JPA: representa una tabla
@Entity
// @Table: nombre exacto de la tabla en la base de datos
@Table(name = "movimientos_inventario")
// @Getter: Lombok genera los metodos getX() de todos los atributos al compilar
@Getter
// @Setter: Lombok genera los metodos setX(valor) de todos los atributos
@Setter
// @NoArgsConstructor: Lombok genera el constructor vacio (lo necesitan JPA y el conversor JSON)
@NoArgsConstructor
public class MovimientoInventario {

    // @Id: este atributo es la clave primaria
    @Id
    // @GeneratedValue: el id lo genera la base (columna IDENTITY / autoincremental)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToOne: relacion muchos-a-uno (llave foranea). LAZY = el otro objeto se carga solo si se usa
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // @JoinColumn: nombre de la columna que guarda la llave foranea
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    // @Enumerated: guarda el enum como texto (su nombre) y no como numero
    @Enumerated(EnumType.STRING)
    // @Column: ajusta la columna: precision/scale para decimales, length para textos
    @Column(length = 25)
    private TipoMovimiento tipo;

    /* Siempre positiva: el tipo indica si suma o resta. */
    private int cantidad;

    /* Stock del lote despues del movimiento. */
    private int stockResultante;

    private String motivo;

    /* Documento relacionado, por ejemplo el numero de venta. */
    private String referencia;

    private String claveIdempotencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private LocalDateTime fecha;
}
