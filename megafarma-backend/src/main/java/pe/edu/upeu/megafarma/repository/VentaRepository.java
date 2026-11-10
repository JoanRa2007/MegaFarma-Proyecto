package pe.edu.upeu.megafarma.repository;

/*
 * PARA ESTUDIAR · CAPA REPOSITORIO (repository)
 * Es el unico lugar que habla con la base de datos. Solo se declara la interfaz:
 * Spring Data JPA escribe la implementacion al arrancar. Los metodos como
 * findByUsername se traducen solos a SQL a partir de su nombre.
 */

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.megafarma.entity.Venta;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// JpaRepository<Entidad, TipoDelId> ya trae save, findById, findAll, delete y count
public interface VentaRepository extends JpaRepository<Venta, Long> {

    // Optional: caja que puede traer un valor o venir vacia; evita trabajar con null
    Optional<Venta> findByClaveIdempotencia(String claveIdempotencia);

    /* Ventas de un rango de fechas: desde (inclusive) hasta (exclusive). */
    // @Query: consulta escrita a mano en JPQL (se consulta sobre entidades, no sobre tablas)
    @Query("SELECT v FROM Venta v WHERE v.fecha >= :desde AND v.fecha < :hasta")
    Page<Venta> porRango(@Param("desde") LocalDateTime desde,
                         // @Param: une el parametro Java con el :nombre usado dentro de la consulta
                         @Param("hasta") LocalDateTime hasta,
                         Pageable pageable);

    @Query("SELECT v FROM Venta v WHERE v.fecha >= :desde AND v.fecha < :hasta ORDER BY v.fecha ASC")
    List<Venta> todasPorRango(@Param("desde") LocalDateTime desde,
                              @Param("hasta") LocalDateTime hasta);
}
