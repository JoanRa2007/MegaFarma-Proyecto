package pe.edu.upeu.megafarma.controller;

/*
 * PARA ESTUDIAR · CAPA CONTROLADOR (controller)
 * Inicio de sesion (publico), cierre de sesion y perfil del usuario.
 */

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.megafarma.dto.auth.CambioPasswordRequestDTO;
import pe.edu.upeu.megafarma.dto.auth.LoginRequestDTO;
import pe.edu.upeu.megafarma.dto.auth.LoginResponseDTO;
import pe.edu.upeu.megafarma.dto.auth.UsuarioResponseDTO;
import pe.edu.upeu.megafarma.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "1. Autenticación", description = "Inicio y cierre de sesión con JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión (HU-21)",
            description = "Devuelve un token JWT. Envíelo en la cabecera Authorization: Bearer <token>. "
                    + "Tras varios fallos desde la misma IP responde 429.")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request,
                                                  HttpServletRequest http) {
        return ResponseEntity.ok(authService.login(request, http.getRemoteAddr()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión: el token queda revocado aunque no haya expirado")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String cabecera) {
        authService.logout(cabecera.substring("Bearer ".length()).trim());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/password")
    @Operation(summary = "Cambiar la propia contraseña",
            description = "Pide la contraseña actual. Quita la marca de contraseña temporal.")
    public ResponseEntity<Void> cambiarPassword(@Valid @RequestBody CambioPasswordRequestDTO request) {
        authService.cambiarPassword(request.getActual(), request.getNueva());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Datos, rol y permisos del usuario autenticado")
    public ResponseEntity<UsuarioResponseDTO> perfil() {
        return ResponseEntity.ok(authService.perfil());
    }
}
