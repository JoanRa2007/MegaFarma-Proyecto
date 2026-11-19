package pe.edu.upeu.megafarma.repository;

/*
 * PARA ESTUDIAR · CAPA REPOSITORIO (repository)
 * Comprobantes electronicos emitidos.
 */

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.megafarma.entity.Comprobante;

import java.util.Optional;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    Optional<Comprobante> findByVentaId(Long ventaId);
}
