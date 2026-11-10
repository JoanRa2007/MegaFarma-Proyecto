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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.megafarma.dto.PaginaResponseDTO;
import pe.edu.upeu.megafarma.dto.inventario.MotivoRequestDTO;
import pe.edu.upeu.megafarma.dto.venta.VentaRequestDTO;
import pe.edu.upeu.megafarma.dto.venta.VentaResponseDTO;
import pe.edu.upeu.megafarma.service.VentaService;

import java.time.LocalDate;

// @RestController: controlador REST: lo que devuelven sus metodos se convierte a JSON
@RestController
// @RequestMapping: ruta base comun a todos los metodos de esta clase
@RequestMapping("/api/v1/ventas")
// @Tag: nombre del grupo con que aparece este controlador en Swagger
@Tag(name = "7. Ventas", description = "Registro con FEFO, consulta y anulación")
public class VentaController {

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.ventaService = ventaService;
    }

    // @PostMapping: atiende peticiones HTTP POST (crear o ejecutar una accion)
    @PostMapping
    // @Operation: titulo y descripcion de la operacion en Swagger
    @Operation(summary = "Registrar una venta (HU-09, HU-15, HU-29, HU-34)",
            description = "Descuenta el stock del lote que vence primero (FEFO) y nunca de lotes vencidos. "
                    + "Con 'claveIdempotencia' un reenvío devuelve la venta ya registrada.")
    public ResponseEntity<VentaResponseDTO> registrar(@Valid @RequestBody VentaRequestDTO request) {
        // responde HTTP 201 Created con el recurso recien creado
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.registrar(request));
    }

    // @GetMapping: atiende peticiones HTTP GET (leer)
    @GetMapping
    @Operation(summary = "Buscar ventas por rango de fechas (HU-11)",
            description = "Fechas en formato AAAA-MM-DD. Por defecto, el mes actual.")
    public ResponseEntity<PaginaResponseDTO<VentaResponseDTO>> listar(
            // @RequestParam: toma el valor de la query string: ?nombre=valor
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        // responde HTTP 200 OK; el objeto se convierte a JSON
        return ResponseEntity.ok(ventaService.listar(desde, hasta, pagina, tamanio));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar el detalle de una venta (HU-10)")
    public ResponseEntity<VentaResponseDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.buscar(id));
    }

    @PostMapping("/{id}/anular")
    @Operation(summary = "Anular una venta y devolver el stock a sus lotes (HU-12, solo ADMIN)")
    public ResponseEntity<VentaResponseDTO> anular(
            // @PathVariable: toma el valor de la ruta, por ejemplo el {id}
            @PathVariable Long id,
            // @Valid: ejecuta las validaciones del DTO; si alguna falla responde 400 sin entrar al metodo
            @Valid @RequestBody MotivoRequestDTO request) {
        return ResponseEntity.ok(ventaService.anular(id, request.getMotivo()));
    }
}
