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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// @Entity: esta clase es una entidad JPA: representa una tabla
@Entity
// @Table: nombre exacto de la tabla en la base de datos
@Table(name = "productos")
// @Getter: Lombok genera los metodos getX() de todos los atributos al compilar
@Getter
// @Setter: Lombok genera los metodos setX(valor) de todos los atributos
@Setter
// @NoArgsConstructor: Lombok genera el constructor vacio (lo necesitan JPA y el conversor JSON)
@NoArgsConstructor
public class Producto {

    // @Id: este atributo es la clave primaria
    @Id
    // @GeneratedValue: el id lo genera la base (columna IDENTITY / autoincremental)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo;

    private String nombre;

    private String principioActivo;

    private String presentacion;

    private String laboratorio;

    // @Column: ajusta la columna: precision/scale para decimales, length para textos
    @Column(precision = 10, scale = 2)
    // BigDecimal: decimales exactos para dinero (double pierde centimos por redondeo)
    private BigDecimal precioVenta;

    private int stockMinimo;

    private boolean requiereReceta;

    private boolean activo = true;

    // @ManyToOne: relacion muchos-a-uno (llave foranea). LAZY = el otro objeto se carga solo si se usa
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // @JoinColumn: nombre de la columna que guarda la llave foranea
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    private LocalDateTime creadoEn;

    /* Sirve a la app movil para sincronizar solo lo que cambio (offline-first). */
    private LocalDateTime actualizadoEn;

    // @PrePersist: metodo que JPA ejecuta justo antes del INSERT
    @PrePersist
    void alCrear() {
        LocalDateTime ahora = LocalDateTime.now();
        if (creadoEn == null) {
            creadoEn = ahora;
        }
        actualizadoEn = ahora;
    }

    // @PreUpdate: metodo que JPA ejecuta justo antes del UPDATE
    @PreUpdate
    void alActualizar() {
        actualizadoEn = LocalDateTime.now();
    }
}
