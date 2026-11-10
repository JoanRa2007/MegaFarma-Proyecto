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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.megafarma.dto.inventario.AjusteRequestDTO;
import pe.edu.upeu.megafarma.dto.inventario.EntradaRequestDTO;
import pe.edu.upeu.megafarma.dto.inventario.LoteResponseDTO;
import pe.edu.upeu.megafarma.dto.inventario.MotivoRequestDTO;
import pe.edu.upeu.megafarma.dto.inventario.MovimientoResponseDTO;
import pe.edu.upeu.megafarma.dto.inventario.StockProductoDTO;
import pe.edu.upeu.megafarma.service.InventarioService;

import java.util.List;

// @RestController: controlador REST: lo que devuelven sus metodos se convierte a JSON
@RestController
// @RequestMapping: ruta base comun a todos los metodos de esta clase
@RequestMapping("/api/v1/inventario")
// @Tag: nombre del grupo con que aparece este controlador en Swagger
@Tag(name = "5. Inventario", description = "Entradas por lote, stock, ajustes, bajas y kardex")
public class InventarioController {

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.inventarioService = inventarioService;
    }

    // @PostMapping: atiende peticiones HTTP POST (crear o ejecutar una accion)
    @PostMapping("/entradas")
    // @Operation: titulo y descripcion de la operacion en Swagger
    @Operation(summary = "Registrar una entrada de mercadería por lote (HU-05)",
            description = "Mismo lote y misma fecha de vencimiento suma la cantidad. "
                    + "Con 'claveIdempotencia' un reenvío no duplica el stock.")
    public ResponseEntity<LoteResponseDTO> registrarEntrada(
            // @Valid: ejecuta las validaciones del DTO; si alguna falla responde 400 sin entrar al metodo
            @Valid @RequestBody EntradaRequestDTO request) {
        // responde HTTP 201 Created con el recurso recien creado
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventarioService.registrarEntrada(request));
    }

    // @GetMapping: atiende peticiones HTTP GET (leer)
    @GetMapping("/stock")
    @Operation(summary = "Stock general de todos los productos activos")
    public ResponseEntity<List<StockProductoDTO>> stockGeneral() {
        // responde HTTP 200 OK; el objeto se convierte a JSON
        return ResponseEntity.ok(inventarioService.stockGeneral());
    }

    @GetMapping("/stock/{productoId}")
    @Operation(summary = "Stock de un producto con el detalle de sus lotes (HU-06)")
    public ResponseEntity<StockProductoDTO> stockDeProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(inventarioService.stockDeProducto(productoId));
    }

    @PostMapping("/ajustes")
    @Operation(summary = "Ajustar el stock de un lote según el conteo físico (HU-07)")
    public ResponseEntity<LoteResponseDTO> ajustar(@Valid @RequestBody AjusteRequestDTO request) {
        return ResponseEntity.ok(inventarioService.ajustar(request));
    }

    @PostMapping("/lotes/{loteId}/baja")
    @Operation(summary = "Dar de baja un lote vencido (HU-16)")
    public ResponseEntity<LoteResponseDTO> darDeBaja(
            // @PathVariable: toma el valor de la ruta, por ejemplo el {id}
            @PathVariable Long loteId,
            @Valid @RequestBody MotivoRequestDTO request) {
        return ResponseEntity.ok(inventarioService.darDeBaja(loteId, request.getMotivo()));
    }

    @GetMapping("/kardex/{productoId}")
    @Operation(summary = "Kardex: historial de movimientos de un producto (HU-37)")
    public ResponseEntity<List<MovimientoResponseDTO>> kardex(@PathVariable Long productoId) {
        return ResponseEntity.ok(inventarioService.kardex(productoId));
    }
}
