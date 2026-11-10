package pe.edu.upeu.megafarma.controller;

/*
 * PARA ESTUDIAR · CAPA CONTROLADOR (controller)
 * Puerta de entrada HTTP. Recibe la peticion, valida el JSON de entrada (@Valid),
 * llama al servicio y devuelve la respuesta con su codigo HTTP. No tiene reglas
 * de negocio: solo traduce entre HTTP y el servicio.
 */

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.megafarma.dto.inventario.LoteResponseDTO;
import pe.edu.upeu.megafarma.dto.inventario.ResumenAlertasDTO;
import pe.edu.upeu.megafarma.dto.inventario.StockProductoDTO;
import pe.edu.upeu.megafarma.service.InventarioService;

import java.util.List;

// @RestController: controlador REST: lo que devuelven sus metodos se convierte a JSON
@RestController
// @RequestMapping: ruta base comun a todos los metodos de esta clase
@RequestMapping("/api/v1/alertas")
// @Tag: nombre del grupo con que aparece este controlador en Swagger
@Tag(name = "6. Alertas", description = "Stock mínimo y vencimientos")
public class AlertaController {

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final InventarioService inventarioService;

    public AlertaController(InventarioService inventarioService) {
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.inventarioService = inventarioService;
    }

    // @GetMapping: atiende peticiones HTTP GET (leer)
    @GetMapping("/stock-minimo")
    // @Operation: titulo y descripcion de la operacion en Swagger
    @Operation(summary = "Productos en o por debajo de su stock mínimo (HU-08)")
    public ResponseEntity<List<StockProductoDTO>> stockMinimo() {
        // responde HTTP 200 OK; el objeto se convierte a JSON
        return ResponseEntity.ok(inventarioService.alertasStockMinimo());
    }

    @GetMapping("/vencimientos")
    @Operation(summary = "Lotes vencidos o que vencen en los próximos N días (HU-13)",
            description = "Si no se envía 'dias' se usa el valor configurado (60).")
    public ResponseEntity<List<LoteResponseDTO>> vencimientos(
            // @RequestParam: toma el valor de la query string: ?nombre=valor
            @RequestParam(required = false) Integer dias) {
        return ResponseEntity.ok(inventarioService.alertasVencimiento(dias));
    }

    @GetMapping("/resumen")
    @Operation(summary = "Contadores de alertas para el panel de inicio (HU-41)")
    public ResponseEntity<ResumenAlertasDTO> resumen(
            @RequestParam(required = false) Integer dias) {
        return ResponseEntity.ok(inventarioService.resumenAlertas(dias));
    }
}
