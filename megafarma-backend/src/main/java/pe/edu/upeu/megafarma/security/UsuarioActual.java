package pe.edu.upeu.megafarma.security;

/*
 * PARA ESTUDIAR · SEGURIDAD (security)
 * Clases de autenticacion (quien eres: token JWT) y autorizacion (que puedes
 * hacer: permisos del rol).
 */

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import pe.edu.upeu.megafarma.entity.Usuario;
import pe.edu.upeu.megafarma.exception.RecursoNoEncontradoException;
import pe.edu.upeu.megafarma.repository.UsuarioRepository;

/* Devuelve el usuario que hizo la peticion (el dueno del token). */
// @Component: Spring crea un unico objeto de esta clase y lo inyecta donde se necesite
@Component
public class UsuarioActual {

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final UsuarioRepository usuarioRepository;

    public UsuarioActual(UsuarioRepository usuarioRepository) {
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario obtener() {
        // SecurityContextHolder: guarda quien es el usuario autenticado durante la peticion
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        String username = autenticacion == null ? null : autenticacion.getName();

        return usuarioRepository.findByUsername(username)
                // findBy... devuelve un Optional: si viene vacio se lanza la excepcion indicada
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el usuario autenticado"));
    }
}
