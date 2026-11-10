package pe.edu.upeu.megafarma.repository;

/*
 * PARA ESTUDIAR · CAPA REPOSITORIO (repository)
 * Es el unico lugar que habla con la base de datos. Solo se declara la interfaz:
 * Spring Data JPA escribe la implementacion al arrancar. Los metodos como
 * findByUsername se traducen solos a SQL a partir de su nombre.
 */

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.megafarma.entity.MovimientoInventario;
import pe.edu.upeu.megafarma.enums.TipoMovimiento;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// JpaRepository<Entidad, TipoDelId> ya trae save, findById, findAll, delete y count
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    // Optional: caja que puede traer un valor o venir vacia; evita trabajar con null
    Optional<MovimientoInventario> findByClaveIdempotencia(String claveIdempotencia);

    // @Query: consulta escrita a mano en JPQL (se consulta sobre entidades, no sobre tablas)
    @Query("""
            SELECT m FROM MovimientoInventario m
            JOIN FETCH m.producto JOIN FETCH m.lote JOIN FETCH m.usuario
            WHERE m.producto.id = :productoId
            ORDER BY m.fecha DESC, m.id DESC
            """)
    List<MovimientoInventario> kardex(@Param("productoId") Long productoId);

    @Query("""
            SELECT m FROM MovimientoInventario m
            JOIN FETCH m.producto JOIN FETCH m.lote JOIN FETCH m.usuario
            WHERE m.tipo = :tipo AND m.fecha >= :desde AND m.fecha < :hasta
            ORDER BY m.fecha DESC, m.id DESC
            """)
    List<MovimientoInventario> porTipoYFecha(@Param("tipo") TipoMovimiento tipo,
                                             // @Param: une el parametro Java con el :nombre usado dentro de la consulta
                                             @Param("desde") LocalDateTime desde,
                                             @Param("hasta") LocalDateTime hasta);
}
