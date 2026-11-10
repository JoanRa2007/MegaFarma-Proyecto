package pe.edu.upeu.megafarma.controller;

/*
 * PARA ESTUDIAR · CAPA CONTROLADOR (controller)
 * Puerta de entrada HTTP. Recibe la peticion, valida el JSON de entrada (@Valid),
 * llama al servicio y devuelve la respuesta con su codigo HTTP. No tiene reglas
 * de negocio: solo traduce entre HTTP y el servicio.
 */

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.megafarma.dto.auth.RolResponseDTO;
import pe.edu.upeu.megafarma.dto.auth.UsuarioRequestDTO;
import pe.edu.upeu.megafarma.dto.auth.UsuarioResponseDTO;
import pe.edu.upeu.megafarma.service.UsuarioService;

import java.util.List;

// @RestController: controlador REST: lo que devuelven sus metodos se convierte a JSON
@RestController
// @RequestMapping: ruta base comun a todos los metodos de esta clase
@RequestMapping("/api/v1")
// @Tag: nombre del grupo con que aparece este controlador en Swagger
@Tag(name = "8. Usuarios y roles", description = "Administración de cuentas (solo ADMIN)")
public class UsuarioController {

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.usuarioService = usuarioService;
    }

    // @GetMapping: atiende peticiones HTTP GET (leer)
    @GetMapping("/usuarios")
    // @Operation: titulo y descripcion de la operacion en Swagger
    @Operation(summary = "Listar usuarios")
    public ResponseEntity<List<UsuarioResponseDTO>> listar() {
        // responde HTTP 200 OK; el objeto se convierte a JSON
        return ResponseEntity.ok(usuarioService.listar());
    }

    @GetMapping("/usuarios/{id}")
    @Operation(summary = "Buscar un usuario por id")
    public ResponseEntity<UsuarioResponseDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscar(id));
    }

    // @PostMapping: atiende peticiones HTTP POST (crear o ejecutar una accion)
    @PostMapping("/usuarios")
    @Operation(summary = "Crear una cuenta de usuario (HU-22)")
    public ResponseEntity<UsuarioResponseDTO> crear(@Valid @RequestBody UsuarioRequestDTO request) {
        // responde HTTP 201 Created con el recurso recien creado
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    // @PutMapping: atiende peticiones HTTP PUT (actualizar todo el recurso)
    @PutMapping("/usuarios/{id}")
    @Operation(summary = "Actualizar datos y rol de un usuario (HU-23)",
            description = "La contraseña solo cambia si se envía una nueva.")
    public ResponseEntity<UsuarioResponseDTO> actualizar(
            // @PathVariable: toma el valor de la ruta, por ejemplo el {id}
            @PathVariable Long id,
            // @Valid: ejecuta las validaciones del DTO; si alguna falla responde 400 sin entrar al metodo
            @Valid @RequestBody UsuarioRequestDTO request) {
        return ResponseEntity.ok(usuarioService.actualizar(id, request));
    }

    // @PatchMapping: atiende peticiones HTTP PATCH (cambiar una parte del recurso)
    @PatchMapping("/usuarios/{id}/estado")
    @Operation(summary = "Activar o desactivar una cuenta (HU-24)",
            description = "Reactivar una cuenta también quita el bloqueo por intentos fallidos.")
    public ResponseEntity<UsuarioResponseDTO> cambiarEstado(
            @PathVariable Long id,
            // @RequestParam: toma el valor de la query string: ?nombre=valor
            @RequestParam boolean activo) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(id, activo));
    }

    @GetMapping("/roles")
    @Operation(summary = "Listar roles con sus permisos")
    public ResponseEntity<List<RolResponseDTO>> roles() {
        return ResponseEntity.ok(usuarioService.roles());
    }
}
