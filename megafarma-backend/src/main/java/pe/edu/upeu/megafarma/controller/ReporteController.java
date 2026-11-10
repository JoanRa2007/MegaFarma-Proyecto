package pe.edu.upeu.megafarma.controller;

/*
 * PARA ESTUDIAR · CAPA CONTROLADOR (controller)
 * Puerta de entrada HTTP. Recibe la peticion, valida el JSON de entrada (@Valid),
 * llama al servicio y devuelve la respuesta con su codigo HTTP. No tiene reglas
 * de negocio: solo traduce entre HTTP y el servicio.
 */

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.megafarma.dto.inventario.MovimientoResponseDTO;
import pe.edu.upeu.megafarma.dto.reporte.ProductoMasVendidoDTO;
import pe.edu.upeu.megafarma.dto.reporte.ReporteInventarioDTO;
import pe.edu.upeu.megafarma.dto.reporte.ReporteVentasDTO;
import pe.edu.upeu.megafarma.service.ReporteService;

import java.time.LocalDate;
import java.util.List;

// @RestController: controlador REST: lo que devuelven sus metodos se convierte a JSON
@RestController
// @RequestMapping: ruta base comun a todos los metodos de esta clase
@RequestMapping("/api/v1/reportes")
// @Tag: nombre del grupo con que aparece este controlador en Swagger
@Tag(name = "9. Reportes", description = "Reportes de gestión (solo ADMIN)")
public class ReporteController {

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.reporteService = reporteService;
    }

    // @GetMapping: atiende peticiones HTTP GET (leer)
    @GetMapping("/ventas")
    // @Operation: titulo y descripcion de la operacion en Swagger
    @Operation(summary = "Reporte de ventas por rango de fechas (HU-17)")
    public ResponseEntity<ReporteVentasDTO> ventas(
            // @RequestParam: toma el valor de la query string: ?nombre=valor
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        // responde HTTP 200 OK; el objeto se convierte a JSON
        return ResponseEntity.ok(reporteService.ventas(desde, hasta));
    }

    @GetMapping("/mas-vendidos")
    @Operation(summary = "Productos más vendidos (HU-19)")
    public ResponseEntity<List<ProductoMasVendidoDTO>> masVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(reporteService.masVendidos(desde, hasta, limite));
    }

    @GetMapping("/inventario")
    @Operation(summary = "Reporte de inventario valorizado (HU-18)")
    public ResponseEntity<ReporteInventarioDTO> inventario() {
        return ResponseEntity.ok(reporteService.inventario());
    }

    @GetMapping("/bajas")
    @Operation(summary = "Reporte de bajas por vencimiento (HU-20)")
    public ResponseEntity<List<MovimientoResponseDTO>> bajas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.bajasPorVencimiento(desde, hasta));
    }
}
