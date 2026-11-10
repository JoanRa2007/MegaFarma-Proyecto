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
import pe.edu.upeu.megafarma.entity.Producto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// JpaRepository<Entidad, TipoDelId> ya trae save, findById, findAll, delete y count
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Optional: caja que puede traer un valor o venir vacia; evita trabajar con null
    Optional<Producto> findByCodigoIgnoreCase(String codigo);

    boolean existsByCategoriaId(Long categoriaId);

    List<Producto> findByActivoTrueOrderByNombreAsc();

    /* Sincronizacion incremental para la app movil (offline-first). */
    List<Producto> findByActualizadoEnAfterOrderByActualizadoEnAsc(LocalDateTime desde);

    /*
     * Busqueda por codigo, nombre o principio activo (HU-03).
     * El patron llega ya en minusculas y con comodines: "%texto%".
     */
    // @Query: consulta escrita a mano en JPQL (se consulta sobre entidades, no sobre tablas)
    @Query("""
            SELECT p FROM Producto p
            WHERE (:incluirInactivos = true OR p.activo = true)
              AND (LOWER(p.codigo) LIKE :patron
                   OR LOWER(p.nombre) LIKE :patron
                   OR LOWER(COALESCE(p.principioActivo, '')) LIKE :patron)
            """)
    Page<Producto> buscar(@Param("patron") String patron,
                          // @Param: une el parametro Java con el :nombre usado dentro de la consulta
                          @Param("incluirInactivos") boolean incluirInactivos,
                          Pageable pageable);
}
