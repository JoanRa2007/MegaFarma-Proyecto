package pe.edu.upeu.megafarma.security;

/*
 * PARA ESTUDIAR · SEGURIDAD (security)
 * Clases de autenticacion (quien eres: token JWT) y autorizacion (que puedes
 * hacer: permisos del rol).
 */

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.megafarma.entity.Usuario;
import pe.edu.upeu.megafarma.repository.UsuarioRepository;

// @Service: Spring crea un unico objeto de esta clase y lo inyecta donde se necesite
@Service
// implements: esta clase cumple el contrato (los metodos) de esa interfaz
public class UsuarioDetailsService implements UserDetailsService {

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final UsuarioRepository usuarioRepository;
    private final int maxIntentosFallidos;

    public UsuarioDetailsService(
            UsuarioRepository usuarioRepository,
            // @Value: inyecta un valor de application.yaml (lo que va tras ':' es el valor por defecto)
            @Value("${app.seguridad.max-intentos-fallidos:5}") int maxIntentosFallidos) {
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.usuarioRepository = usuarioRepository;
        this.maxIntentosFallidos = maxIntentosFallidos;
    }

    /*
     * Carga el usuario para Spring Security. Una cuenta desactivada (HU-24)
     * queda "disabled" y una con demasiados intentos fallidos (HU-46)
     * queda "locked": en ambos casos el login y el token se rechazan.
     */
    // @Override: este metodo implementa o redefine uno declarado en la interfaz o clase padre
    @Override
    // @Transactional: todo el metodo es una transaccion: si lanza excepcion se deshacen los cambios (rollback)
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository.findByUsername(username)
                // findBy... devuelve un Optional: si viene vacio se lanza la excepcion indicada
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return User.withUsername(usuario.getUsername())
                .password(usuario.getPasswordHash())
                .authorities(PermisosPorRol.authorities(usuario.getRol().getNombre()))
                .disabled(!usuario.isActivo())
                .accountLocked(usuario.getIntentosFallidos() >= maxIntentosFallidos)
                .build();
    }
}
