package pe.edu.upeu.megafarma.repository;

/*
 * PARA ESTUDIAR · CAPA REPOSITORIO (repository)
 * Bitacora de auditoria. Los filtros vacios ('') significan "todos".
 * Los valores llegan como parametros (:usuario), nunca concatenados al SQL.
 */

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.megafarma.entity.Auditoria;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    @Query("""
            SELECT a FROM Auditoria a
            WHERE (:usuario = '' OR a.usuario = :usuario)
              AND (:accion = '' OR a.accion = :accion)
            """)
    Page<Auditoria> buscar(@Param("usuario") String usuario, @Param("accion") String accion, Pageable pageable);
}
