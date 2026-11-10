package pe.edu.upeu.megafarma.service.impl;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - IMPLEMENTACION (service.impl)
 * Aqui viven las reglas de negocio. El controlador llama al servicio, el servicio
 * valida las reglas, usa los repositorios y devuelve un DTO de respuesta.
 */

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.megafarma.dto.auth.RolResponseDTO;
import pe.edu.upeu.megafarma.dto.auth.UsuarioRequestDTO;
import pe.edu.upeu.megafarma.dto.auth.UsuarioResponseDTO;
import pe.edu.upeu.megafarma.entity.Rol;
import pe.edu.upeu.megafarma.entity.Usuario;
import pe.edu.upeu.megafarma.exception.RecursoNoEncontradoException;
import pe.edu.upeu.megafarma.exception.ReglaNegocioException;
import pe.edu.upeu.megafarma.mapper.UsuarioMapper;
import pe.edu.upeu.megafarma.repository.RolRepository;
import pe.edu.upeu.megafarma.repository.UsuarioRepository;
import pe.edu.upeu.megafarma.security.UsuarioActual;
import pe.edu.upeu.megafarma.service.UsuarioService;

import java.util.List;

// @Service: Spring crea un unico objeto de esta clase y lo inyecta donde se necesite
@Service
// implements: esta clase cumple el contrato (los metodos) de esa interfaz
public class UsuarioServiceImpl implements UsuarioService {

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioActual usuarioActual;
    private final int maxIntentosFallidos;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository,
                              RolRepository rolRepository,
                              PasswordEncoder passwordEncoder,
                              UsuarioActual usuarioActual,
                              // @Value: inyecta un valor de application.yaml (lo que va tras ':' es el valor por defecto)
                              @Value("${app.seguridad.max-intentos-fallidos:5}") int maxIntentosFallidos) {
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioActual = usuarioActual;
        this.maxIntentosFallidos = maxIntentosFallidos;
    }

    // @Override: este metodo implementa o redefine uno declarado en la interfaz o clase padre
    @Override
    // @Transactional: todo el metodo es una transaccion: si lanza excepcion se deshacen los cambios (rollback)
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listar() {
        // stream: recorre la coleccion aplicando map/filter y junta el resultado
        return usuarioRepository.findAll(Sort.by("username")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    // readOnly: transaccion solo de lectura (mas liviana, no guarda cambios)
    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscar(Long id) {
        return toResponse(obtener(id));
    }

    /* HU-22 Crear una cuenta de usuario. */
    @Override
    @Transactional
    public UsuarioResponseDTO crear(UsuarioRequestDTO request) {

        String username = request.getUsername().trim().toLowerCase();

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            // regla de negocio incumplida -> el cliente recibe HTTP 409 con este mensaje
            throw new ReglaNegocioException("La contraseña es obligatoria al crear un usuario");
        }
        if (usuarioRepository.existsByUsername(username)) {
            throw new ReglaNegocioException("Ya existe un usuario con el nombre '" + username + "'");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        // BCrypt convierte la contrasena en un hash irreversible antes de guardarla
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setNombreCompleto(request.getNombreCompleto().trim());
        usuario.setRol(obtenerRol(request.getRol()));
        usuario.setActivo(true);

        return toResponse(usuarioRepository.save(usuario));
    }

    /* HU-23 Asignar rol. La contrasena solo cambia si llega una nueva. */
    @Override
    @Transactional
    public UsuarioResponseDTO actualizar(Long id, UsuarioRequestDTO request) {

        Usuario usuario = obtener(id);
        String username = request.getUsername().trim().toLowerCase();

        if (!usuario.getUsername().equals(username)
                && usuarioRepository.existsByUsername(username)) {
            throw new ReglaNegocioException("Ya existe un usuario con el nombre '" + username + "'");
        }

        Rol rol = obtenerRol(request.getRol());
        boolean esElMismo = usuario.getId().equals(usuarioActual.obtener().getId());
        if (esElMismo && !usuario.getRol().getNombre().equals(rol.getNombre())) {
            throw new ReglaNegocioException("No puede cambiar su propio rol");
        }

        usuario.setUsername(username);
        usuario.setNombreCompleto(request.getNombreCompleto().trim());
        usuario.setRol(rol);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        return toResponse(usuarioRepository.save(usuario));
    }

    /*
     * HU-24 Desactivar una cuenta. No se borra para conservar el historial
     * de ventas y movimientos. Reactivarla tambien quita el bloqueo.
     */
    @Override
    @Transactional
    public UsuarioResponseDTO cambiarEstado(Long id, boolean activo) {

        Usuario usuario = obtener(id);

        if (!activo && usuario.getId().equals(usuarioActual.obtener().getId())) {
            throw new ReglaNegocioException("No puede desactivar su propia cuenta");
        }

        usuario.setActivo(activo);
        if (activo) {
            usuario.setIntentosFallidos(0);
        }

        return toResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolResponseDTO> roles() {
        return rolRepository.findAll(Sort.by("nombre")).stream()
                .map(UsuarioMapper::toResponse)
                .toList();
    }

    private Usuario obtener(Long id) {
        return usuarioRepository.findById(id)
                // findBy... devuelve un Optional: si viene vacio se lanza la excepcion indicada
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el usuario con id " + id));
    }

    private Rol obtenerRol(String nombre) {
        return rolRepository.findByNombre(nombre.trim().toUpperCase())
                .orElseThrow(() -> new ReglaNegocioException(
                        "El rol '" + nombre + "' no existe. Use ADMIN, VENDEDOR o ALMACENERO"));
    }

    private UsuarioResponseDTO toResponse(Usuario usuario) {
        return UsuarioMapper.toResponse(usuario, maxIntentosFallidos);
    }
}
