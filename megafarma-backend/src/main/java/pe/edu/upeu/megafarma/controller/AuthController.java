package pe.edu.upeu.megafarma.controller;

/*
 * PARA ESTUDIAR · CAPA CONTROLADOR (controller)
 * Puerta de entrada HTTP. Recibe la peticion, valida el JSON de entrada (@Valid),
 * llama al servicio y devuelve la respuesta con su codigo HTTP. No tiene reglas
 * de negocio: solo traduce entre HTTP y el servicio.
 */

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.megafarma.dto.auth.LoginRequestDTO;
import pe.edu.upeu.megafarma.dto.auth.LoginResponseDTO;
import pe.edu.upeu.megafarma.dto.auth.UsuarioResponseDTO;
import pe.edu.upeu.megafarma.service.AuthService;

// @RestController: controlador REST: lo que devuelven sus metodos se convierte a JSON
@RestController
// @RequestMapping: ruta base comun a todos los metodos de esta clase
@RequestMapping("/api/v1/auth")
// @Tag: nombre del grupo con que aparece este controlador en Swagger
@Tag(name = "1. Autenticación", description = "Inicio de sesión con JWT")
public class AuthController {

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final AuthService authService;

    public AuthController(AuthService authService) {
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.authService = authService;
    }

    // @PostMapping: atiende peticiones HTTP POST (crear o ejecutar una accion)
    @PostMapping("/login")
    // @SecurityRequirements: en Swagger marca esta operacion como publica (no pide token)
    @SecurityRequirements
    // @Operation: titulo y descripcion de la operacion en Swagger
    @Operation(summary = "Iniciar sesión (HU-21)",
            description = "Devuelve un token JWT. Envíelo en la cabecera Authorization: Bearer <token>.")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        // responde HTTP 200 OK; el objeto se convierte a JSON
        return ResponseEntity.ok(authService.login(request));
    }

    // @GetMapping: atiende peticiones HTTP GET (leer)
    @GetMapping("/me")
    @Operation(summary = "Datos, rol y permisos del usuario autenticado")
    public ResponseEntity<UsuarioResponseDTO> perfil() {
        return ResponseEntity.ok(authService.perfil());
    }
}
