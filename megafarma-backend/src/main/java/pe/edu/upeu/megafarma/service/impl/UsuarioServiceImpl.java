package pe.edu.upeu.megafarma.service.impl;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - IMPLEMENTACION (service.impl)
 * Aqui viven las reglas de negocio. El controlador llama al servicio, el servicio
 * valida las reglas, usa los repositorios y devuelve un DTO de respuesta.
 */

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.megafarma.dto.PaginaResponseDTO;
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
import pe.edu.upeu.megafarma.util.PaginacionUtil;

import java.util.List;
import java.util.Set;

// @Service: Spring crea un unico objeto de esta clase y lo inyecta donde se necesite
@Service
// implements: esta clase cumple el contrato (los metodos) de esa interfaz
public class UsuarioServiceImpl implements UsuarioService {

    private static final Set<String> CAMPOS_ORDEN = Set.of("id", "username", "nombreCompleto");

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
    public PaginaResponseDTO<UsuarioResponseDTO> listar(String q, int pagina, int tamanio) {
        Pageable pageable = PaginacionUtil.crear(
                pagina, tamanio, "username", "asc", CAMPOS_ORDEN, "username");
        Page<Usuario> resultado = usuarioRepository.buscar(PaginacionUtil.patron(q), pageable);
        return PaginaResponseDTO.de(resultado,
                resultado.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    // readOnly: transaccion solo de lectura (mas liviana, no guarda cambios)
    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscar(Long id) {
        return toResponse(obtener(id));
    }

    /*
     * HU-22 Crear una cuenta de usuario. Si no llega contrasena, el sistema genera
     * una temporal: se devuelve una sola vez y el trabajador debe cambiarla al entrar.
     */
    @Override
    @Transactional
    public UsuarioResponseDTO crear(UsuarioRequestDTO request) {

        String username = request.getUsername().trim().toLowerCase();

        if (usuarioRepository.existsByUsername(username)) {
            // regla de negocio incumplida -> el cliente recibe HTTP 409 con este mensaje
            throw new ReglaNegocioException("Ya existe un usuario con el nombre '" + username + "'");
        }
        String dni = textoONulo(request.getDni());
        if (dni != null && usuarioRepository.existsByDni(dni)) {
            throw new ReglaNegocioException("Ya existe un usuario con el DNI " + dni);
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        aplicarDatos(usuario, request);
        usuario.setRol(obtenerRol(request.getRol()));
        usuario.setActivo(true);

        String temporal = null;
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            temporal = generarPasswordTemporal();
            // BCrypt convierte la contrasena en un hash irreversible antes de guardarla
            usuario.setPasswordHash(passwordEncoder.encode(temporal));
            usuario.setDebeCambiarPassword(true);
        } else {
            usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        UsuarioResponseDTO respuesta = toResponse(usuarioRepository.save(usuario));
        respuesta.setPasswordTemporal(temporal);
        return respuesta;
    }

    /* HU-23 Asignar rol y datos. La contrasena solo cambia si llega una nueva. */
    @Override
    @Transactional
    public UsuarioResponseDTO actualizar(Long id, UsuarioRequestDTO request) {

        Usuario usuario = obtener(id);
        String username = request.getUsername().trim().toLowerCase();

        if (!usuario.getUsername().equals(username)
                && usuarioRepository.existsByUsername(username)) {
            throw new ReglaNegocioException("Ya existe un usuario con el nombre '" + username + "'");
        }
        String dni = textoONulo(request.getDni());
        if (dni != null && usuarioRepository.existsByDniAndIdNot(dni, id)) {
            throw new ReglaNegocioException("Ya existe un usuario con el DNI " + dni);
        }

        Rol rol = obtenerRol(request.getRol());
        boolean esElMismo = usuario.getId().equals(usuarioActual.obtener().getId());
        if (esElMismo && !usuario.getRol().getNombre().equals(rol.getNombre())) {
            throw new ReglaNegocioException("No puede cambiar su propio rol");
        }

        usuario.setUsername(username);
        aplicarDatos(usuario, request);
        usuario.setRol(rol);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        return toResponse(usuarioRepository.save(usuario));
    }

    /*
     * Recuperar contrasena: el admin genera una clave temporal para el trabajador.
     * Tambien quita el bloqueo por intentos fallidos. El trabajador la cambia al entrar.
     */
    @Override
    @Transactional
    public UsuarioResponseDTO restablecerPassword(Long id) {

        Usuario usuario = obtener(id);
        if (usuario.getId().equals(usuarioActual.obtener().getId())) {
            throw new ReglaNegocioException("Para cambiar su propia contraseña use Mi perfil");
        }

        String temporal = generarPasswordTemporal();
        usuario.setPasswordHash(passwordEncoder.encode(temporal));
        usuario.setDebeCambiarPassword(true);
        usuario.setIntentosFallidos(0);

        UsuarioResponseDTO respuesta = toResponse(usuarioRepository.save(usuario));
        respuesta.setPasswordTemporal(temporal);
        return respuesta;
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

    /* Copia nombres, apellidos, DNI, telefono y correo; arma el nombre completo. */
    private void aplicarDatos(Usuario usuario, UsuarioRequestDTO request) {
        String nombres = textoONulo(request.getNombres());
        String apellidos = textoONulo(request.getApellidos());
        String nombreCompleto;
        if (nombres != null && apellidos != null) {
            nombreCompleto = nombres + " " + apellidos;
        } else if (textoONulo(request.getNombreCompleto()) != null) {
            nombreCompleto = request.getNombreCompleto().trim();
        } else {
            throw new ReglaNegocioException("Ingrese los nombres y apellidos del trabajador");
        }
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        usuario.setNombreCompleto(nombreCompleto);
        usuario.setDni(textoONulo(request.getDni()));
        usuario.setTelefono(textoONulo(request.getTelefono()));
        String correo = textoONulo(request.getCorreo());
        usuario.setCorreo(correo == null ? null : correo.toLowerCase());
    }

    private static String textoONulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    /* 10 caracteres sin letras confusas (0/O, 1/l/I), con al menos una letra y un numero. */
    private static String generarPasswordTemporal() {
        String letras = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
        String numeros = "23456789";
        String todos = letras + numeros;
        StringBuilder clave = new StringBuilder();
        clave.append(letras.charAt(ALEATORIO.nextInt(letras.length())));
        clave.append(numeros.charAt(ALEATORIO.nextInt(numeros.length())));
        while (clave.length() < 10) {
            clave.append(todos.charAt(ALEATORIO.nextInt(todos.length())));
        }
        // mezcla para que la letra y el numero no queden siempre al inicio
        for (int i = clave.length() - 1; i > 0; i--) {
            int j = ALEATORIO.nextInt(i + 1);
            char c = clave.charAt(i);
            clave.setCharAt(i, clave.charAt(j));
            clave.setCharAt(j, c);
        }
        return clave.toString();
    }

    private static final java.security.SecureRandom ALEATORIO = new java.security.SecureRandom();

    private UsuarioResponseDTO toResponse(Usuario usuario) {
        return UsuarioMapper.toResponse(usuario, maxIntentosFallidos);
    }
}
