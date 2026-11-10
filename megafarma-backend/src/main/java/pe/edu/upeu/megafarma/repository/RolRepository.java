package pe.edu.upeu.megafarma.repository;

/*
 * PARA ESTUDIAR · CAPA REPOSITORIO (repository)
 * Es el unico lugar que habla con la base de datos. Solo se declara la interfaz:
 * Spring Data JPA escribe la implementacion al arrancar. Los metodos como
 * findByUsername se traducen solos a SQL a partir de su nombre.
 */

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.megafarma.entity.Rol;

import java.util.Optional;

// JpaRepository<Entidad, TipoDelId> ya trae save, findById, findAll, delete y count
public interface RolRepository extends JpaRepository<Rol, Long> {

    // Optional: caja que puede traer un valor o venir vacia; evita trabajar con null
    Optional<Rol> findByNombre(String nombre);
}
