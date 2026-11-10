package pe.edu.upeu.megafarma.repository;

/*
 * PARA ESTUDIAR · CAPA REPOSITORIO (repository)
 * Es el unico lugar que habla con la base de datos. Solo se declara la interfaz:
 * Spring Data JPA escribe la implementacion al arrancar. Los metodos como
 * findByUsername se traducen solos a SQL a partir de su nombre.
 */

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.megafarma.entity.Lote;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// JpaRepository<Entidad, TipoDelId> ya trae save, findById, findAll, delete y count
public interface LoteRepository extends JpaRepository<Lote, Long> {

    // Optional: caja que puede traer un valor o venir vacia; evita trabajar con null
    Optional<Lote> findByProductoIdAndNumeroLote(Long productoId, String numeroLote);

    List<Lote> findByProductoIdOrderByFechaVencimientoAscIdAsc(Long productoId);

    /*
     * FEFO (First Expired, First Out): lotes vendibles del producto, primero
     * el que vence antes. Se bloquean (SELECT ... FOR UPDATE) para que dos
     * ventas simultaneas no descuenten el mismo stock.
     */
    // @Lock: PESSIMISTIC_WRITE = SELECT ... FOR UPDATE: bloquea las filas hasta terminar la transaccion
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    // @Query: consulta escrita a mano en JPQL (se consulta sobre entidades, no sobre tablas)
    @Query("""
            SELECT l FROM Lote l
            WHERE l.producto.id = :productoId
              AND l.dadoDeBaja = false
              AND l.cantidadActual > 0
              AND l.fechaVencimiento > :hoy
            ORDER BY l.fechaVencimiento ASC, l.id ASC
            """)
    List<Lote> lotesVendiblesFefo(@Param("productoId") Long productoId,
                                  // @Param: une el parametro Java con el :nombre usado dentro de la consulta
                                  @Param("hoy") LocalDate hoy);

    /* Bloquea un lote para modificar su cantidad (ajuste, baja, anulacion). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Lote l WHERE l.id = :id")
    Optional<Lote> buscarParaActualizar(@Param("id") Long id);

    /* [productoId, stock] de lo que se puede vender hoy (sin vencidos ni bajas). */
    @Query("""
            SELECT l.producto.id, SUM(l.cantidadActual) FROM Lote l
            WHERE l.dadoDeBaja = false AND l.fechaVencimiento > :hoy
            GROUP BY l.producto.id
            """)
    List<Object[]> stockVendiblePorProducto(@Param("hoy") LocalDate hoy);

    /* [productoId, stock] de todo lo registrado que no fue dado de baja. */
    @Query("""
            SELECT l.producto.id, SUM(l.cantidadActual) FROM Lote l
            WHERE l.dadoDeBaja = false
            GROUP BY l.producto.id
            """)
    List<Object[]> stockRegistradoPorProducto();

    /* Lotes con stock que vencen hasta la fecha limite (incluye ya vencidos). */
    @Query("""
            SELECT l FROM Lote l JOIN FETCH l.producto
            WHERE l.dadoDeBaja = false
              AND l.cantidadActual > 0
              AND l.fechaVencimiento <= :limite
            ORDER BY l.fechaVencimiento ASC, l.id ASC
            """)
    List<Lote> porVencer(@Param("limite") LocalDate limite);

    @Query("""
            SELECT l FROM Lote l JOIN FETCH l.producto
            WHERE l.dadoDeBaja = false AND l.cantidadActual > 0
            ORDER BY l.producto.nombre ASC, l.fechaVencimiento ASC
            """)
    List<Lote> conStock();
}
