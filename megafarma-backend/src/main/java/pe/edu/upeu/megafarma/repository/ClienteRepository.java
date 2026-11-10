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
import pe.edu.upeu.megafarma.entity.Cliente;

import java.util.Optional;

// JpaRepository<Entidad, TipoDelId> ya trae save, findById, findAll, delete y count
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Optional: caja que puede traer un valor o venir vacia; evita trabajar con null
    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);

    // @Query: consulta escrita a mano en JPQL (se consulta sobre entidades, no sobre tablas)
    @Query("""
            SELECT c FROM Cliente c
            WHERE c.activo = true
              AND (LOWER(c.nombre) LIKE :patron OR c.numeroDocumento LIKE :patron)
            """)
    Page<Cliente> buscar(@Param("patron") String patron, Pageable pageable);
}
