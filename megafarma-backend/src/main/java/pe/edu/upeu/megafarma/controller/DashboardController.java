package pe.edu.upeu.megafarma.controller;

/*
 * PARA ESTUDIAR · CAPA CONTROLADOR (controller)
 * Dashboard de ventas del punto de venta (solo ADMIN).
 */

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.megafarma.dto.reporte.ResumenVentasDTO;
import pe.edu.upeu.megafarma.enums.Periodo;
import pe.edu.upeu.megafarma.service.ResumenVentasService;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "9. Reportes", description = "Reportes de gestión (solo ADMIN)")
public class DashboardController {

    private final ResumenVentasService resumenVentasService;

    public DashboardController(ResumenVentasService resumenVentasService) {
        this.resumenVentasService = resumenVentasService;
    }

    @GetMapping
    @Operation(summary = "Dashboard de ventas: HOY, SEMANA, MES o ANIO",
            description = "Indicadores, comparación con el periodo anterior, gráfico, top 5 de productos, "
                    + "ventas por categoría y por medio de pago.")
    public ResponseEntity<ResumenVentasDTO> dashboard(@RequestParam(defaultValue = "HOY") Periodo periodo) {
        return ResponseEntity.ok(resumenVentasService.dashboard(periodo));
    }
}
