package pe.edu.upeu.megafarma.entity;

/*
 * PARA ESTUDIAR · CAPA ENTIDAD (entity)
 * Cada entidad es una tabla de la base de datos y cada atributo es una columna.
 * JPA/Hibernate convierte filas en objetos Java y objetos en filas.
 * El nombre de la columna sale del atributo: precioVenta -> precio_venta.
 */

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.megafarma.enums.EstadoVenta;
import pe.edu.upeu.megafarma.enums.MedioPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// @Entity: esta clase es una entidad JPA: representa una tabla
@Entity
// @Table: nombre exacto de la tabla en la base de datos
@Table(name = "ventas")
// @Getter: Lombok genera los metodos getX() de todos los atributos al compilar
@Getter
// @Setter: Lombok genera los metodos setX(valor) de todos los atributos
@Setter
// @NoArgsConstructor: Lombok genera el constructor vacio (lo necesitan JPA y el conversor JSON)
@NoArgsConstructor
public class Venta {

    // @Id: este atributo es la clave primaria
    @Id
    // @GeneratedValue: el id lo genera la base (columna IDENTITY / autoincremental)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* Correlativo legible: V-00000001. Se asigna al guardar. */
    private String numero;

    private LocalDateTime fecha;

    // @ManyToOne: relacion muchos-a-uno (llave foranea). LAZY = el otro objeto se carga solo si se usa
    @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn: nombre de la columna que guarda la llave foranea
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // @Enumerated: guarda el enum como texto (su nombre) y no como numero
    @Enumerated(EnumType.STRING)
    // @Column: ajusta la columna: precision/scale para decimales, length para textos
    @Column(length = 15)
    private EstadoVenta estado;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private MedioPago medioPago;

    @Column(precision = 12, scale = 2)
    // BigDecimal: decimales exactos para dinero (double pierde centimos por redondeo)
    private BigDecimal total;

    private String motivoAnulacion;

    private LocalDateTime anuladaEn;

    /* Enviada por la app: reintentar la misma venta no la duplica. */
    private String claveIdempotencia;

    // @OneToMany: relacion uno-a-muchos. cascade = guardar el padre guarda tambien a sus hijos
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    // @OrderBy: orden en que se cargan los elementos de la lista
    @OrderBy("id ASC")
    private List<DetalleVenta> detalles = new ArrayList<>();

    public void agregarDetalle(DetalleVenta detalle) {
        detalle.setVenta(this);
        detalles.add(detalle);
    }
}
