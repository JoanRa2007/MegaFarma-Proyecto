package pe.edu.upeu.megafarma.repository;

/*
 * PARA ESTUDIAR · CAPA REPOSITORIO (repository)
 * Es el unico lugar que habla con la base de datos. Solo se declara la interfaz:
 * Spring Data JPA escribe la implementacion al arrancar. Los metodos como
 * findByUsername se traducen solos a SQL a partir de su nombre.
 */

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.megafarma.entity.DetalleVenta;
import pe.edu.upeu.megafarma.enums.EstadoVenta;

import java.time.LocalDateTime;
import java.util.List;

// JpaRepository<Entidad, TipoDelId> ya trae save, findById, findAll, delete y count
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    /* [productoId, codigo, nombre, unidades, importe] ordenado por unidades vendidas. */
    // @Query: consulta escrita a mano en JPQL (se consulta sobre entidades, no sobre tablas)
    @Query("""
            SELECT d.producto.id, d.producto.codigo, d.producto.nombre,
                   SUM(d.cantidad), SUM(d.subtotal)
            FROM DetalleVenta d
            WHERE d.venta.estado = :estado
              AND d.venta.fecha >= :desde AND d.venta.fecha < :hasta
            GROUP BY d.producto.id, d.producto.codigo, d.producto.nombre
            ORDER BY SUM(d.cantidad) DESC, d.producto.nombre ASC
            """)
    List<Object[]> masVendidos(@Param("estado") EstadoVenta estado,
                               // @Param: une el parametro Java con el :nombre usado dentro de la consulta
                               @Param("desde") LocalDateTime desde,
                               @Param("hasta") LocalDateTime hasta,
                               Pageable pageable);
}
