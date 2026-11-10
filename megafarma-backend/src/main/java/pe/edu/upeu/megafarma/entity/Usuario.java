package pe.edu.upeu.megafarma.entity;

/*
 * PARA ESTUDIAR · CAPA ENTIDAD (entity)
 * Cada entidad es una tabla de la base de datos y cada atributo es una columna.
 * JPA/Hibernate convierte filas en objetos Java y objetos en filas.
 * El nombre de la columna sale del atributo: precioVenta -> precio_venta.
 */

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// @Entity: esta clase es una entidad JPA: representa una tabla
@Entity
// @Table: nombre exacto de la tabla en la base de datos
@Table(name = "usuarios")
// @Getter: Lombok genera los metodos getX() de todos los atributos al compilar
@Getter
// @Setter: Lombok genera los metodos setX(valor) de todos los atributos
@Setter
// @NoArgsConstructor: Lombok genera el constructor vacio (lo necesitan JPA y el conversor JSON)
@NoArgsConstructor
public class Usuario {

    // @Id: este atributo es la clave primaria
    @Id
    // @GeneratedValue: el id lo genera la base (columna IDENTITY / autoincremental)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    /* Hash BCrypt: la contrasena nunca se guarda en texto plano. */
    private String passwordHash;

    private String nombreCompleto;

    private boolean activo = true;

    private int intentosFallidos = 0;

    // @ManyToOne: relacion muchos-a-uno (llave foranea). LAZY = el otro objeto se carga solo si se usa
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    // @JoinColumn: nombre de la columna que guarda la llave foranea
    @JoinColumn(name = "rol_id")
    private Rol rol;

    private LocalDateTime creadoEn;

    // @PrePersist: metodo que JPA ejecuta justo antes del INSERT
    @PrePersist
    void alCrear() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
    }
}
