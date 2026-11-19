package pe.edu.upeu.megafarma.controller;

/*
 * PARA ESTUDIAR · CAPA CONTROLADOR (controller)
 * Consulta de la bitacora de auditoria (solo ADMIN). Es de solo lectura:
 * la API no permite modificar ni borrar registros.
 */

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.megafarma.dto.AuditoriaResponseDTO;
import pe.edu.upeu.megafarma.dto.PaginaResponseDTO;
import pe.edu.upeu.megafarma.service.AuditoriaService;

@RestController
@RequestMapping("/api/v1/auditoria")
@Validated
@Tag(name = "8. Usuarios y roles", description = "Administración de cuentas y bitácora (solo ADMIN)")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    @Operation(summary = "Bitácora de auditoría (HU-45)", description = "Filtros opcionales por usuario y acción.")
    public ResponseEntity<PaginaResponseDTO<AuditoriaResponseDTO>> buscar(
            @RequestParam(required = false) @Pattern(regexp = "^[a-zA-Z0-9._-]{0,50}$", message = "Usuario inválido") String usuario,
            @RequestParam(required = false) @Pattern(regexp = "^[A-Za-z_]{0,40}$", message = "Acción inválida") String accion,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int tamanio) {
        return ResponseEntity.ok(auditoriaService.buscar(usuario, accion, pagina, tamanio));
    }
}
