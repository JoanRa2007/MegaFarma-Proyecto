package pe.edu.upeu.megafarma.dto.reporte;

/*
 * PARA ESTUDIAR · DTO (Data Transfer Object)
 * Resumen de ventas de un periodo para el dashboard y los reportes por
 * dia, mes o año: indicadores, comparacion con el periodo anterior,
 * serie para el grafico, productos mas vendidos, categorias y medios de pago.
 */

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumenVentasDTO {
    private String periodo;
    private String titulo;
    private LocalDate desde;
    private LocalDate hasta;
    /* HORA, DIA o MES: como se agrupa la serie del grafico. */
    private String agrupacion;
    private BigDecimal totalVendido;
    private int cantidadVentas;
    private BigDecimal ticketPromedio;
    private long unidadesVendidas;
    private int ventasAnuladas;
    private BigDecimal totalAnulado;
    private BigDecimal totalPeriodoAnterior;
    /* Variacion porcentual frente al periodo anterior; null si antes no hubo ventas. */
    private Double variacionPorcentaje;
    private List<ReporteVentasDTO.Grupo> serie;
    private List<ProductoMasVendidoDTO> topProductos;
    private List<ReporteVentasDTO.Grupo> porCategoria;
    private List<ReporteVentasDTO.Grupo> porMedioPago;
}
