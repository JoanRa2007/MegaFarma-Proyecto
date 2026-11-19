package pe.edu.upeu.megafarma.repository;

/*
 * PARA ESTUDIAR · CAPA REPOSITORIO (repository)
 * Series de comprobantes. La serie se lee bloqueada para asignar el
 * siguiente correlativo sin duplicados.
 */

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.megafarma.entity.SerieComprobante;

import java.util.Optional;

public interface SerieComprobanteRepository extends JpaRepository<SerieComprobante, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SerieComprobante s WHERE s.serie = :serie")
    Optional<SerieComprobante> buscarParaActualizar(@Param("serie") String serie);
}
