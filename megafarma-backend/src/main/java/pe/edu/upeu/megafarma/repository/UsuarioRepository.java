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
import pe.edu.upeu.megafarma.entity.Usuario;

import java.util.Optional;

// JpaRepository<Entidad, TipoDelId> ya trae save, findById, findAll, delete y count
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Optional: caja que puede traer un valor o venir vacia; evita trabajar con null
    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByDni(String dni);

    boolean existsByDniAndIdNot(String dni, Long id);

    /* Busqueda paginada por nombre, usuario, DNI o rol (tabla de administracion). */
    @Query("""
            SELECT u FROM Usuario u
            WHERE (:patron = '%' OR LOWER(u.username) LIKE :patron
                   OR LOWER(u.nombreCompleto) LIKE :patron
                   OR LOWER(COALESCE(u.dni, '')) LIKE :patron
                   OR LOWER(u.rol.nombre) LIKE :patron)
            """)
    Page<Usuario> buscar(@Param("patron") String patron, Pageable pageable);
}
