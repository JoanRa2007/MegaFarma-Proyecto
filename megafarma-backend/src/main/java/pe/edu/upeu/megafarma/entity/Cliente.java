package pe.edu.upeu.megafarma.entity;

/*
 * PARA ESTUDIAR · CAPA ENTIDAD (entity)
 * Cada entidad es una tabla de la base de datos y cada atributo es una columna.
 * JPA/Hibernate convierte filas en objetos Java y objetos en filas.
 * El nombre de la columna sale del atributo: precioVenta -> precio_venta.
 */

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// @Entity: esta clase es una entidad JPA: representa una tabla
@Entity
// @Table: nombre exacto de la tabla en la base de datos
@Table(name = "clientes")
// @Getter: Lombok genera los metodos getX() de todos los atributos al compilar
@Getter
// @Setter: Lombok genera los metodos setX(valor) de todos los atributos
@Setter
// @NoArgsConstructor: Lombok genera el constructor vacio (lo necesitan JPA y el conversor JSON)
@NoArgsConstructor
public class Cliente {

    // @Id: este atributo es la clave primaria
    @Id
    // @GeneratedValue: el id lo genera la base (columna IDENTITY / autoincremental)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipoDocumento;

    private String numeroDocumento;

    private String nombre;

    private String telefono;

    private boolean activo = true;
}
