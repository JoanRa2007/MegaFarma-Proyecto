package pe.edu.upeu.megafarma.service.impl;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - IMPLEMENTACION (service.impl)
 * Calcula el dashboard y los reportes por dia, mes y año:
 *  - total vendido, numero de ventas, ticket promedio y unidades;
 *  - comparacion con el periodo anterior (variacion %);
 *  - serie para el grafico: por hora (un dia), por dia (semana o mes) o por mes (año);
 *  - top 5 de productos, ventas por categoria, por medio de pago y por vendedor;
 *  - boletas y facturas emitidas, y la serie del periodo anterior para comparar.
 * Solo cuentan las ventas REGISTRADAS; las anuladas se informan aparte.
 */

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.megafarma.dto.reporte.ProductoMasVendidoDTO;
import pe.edu.upeu.megafarma.dto.reporte.ReporteVentasDTO;
import pe.edu.upeu.megafarma.dto.reporte.ResumenVentasDTO;
import pe.edu.upeu.megafarma.entity.DetalleVenta;
import pe.edu.upeu.megafarma.entity.Pago;
import pe.edu.upeu.megafarma.entity.Producto;
import pe.edu.upeu.megafarma.entity.Venta;
import pe.edu.upeu.megafarma.enums.EstadoVenta;
import pe.edu.upeu.megafarma.enums.Periodo;
import pe.edu.upeu.megafarma.enums.TipoComprobante;
import pe.edu.upeu.megafarma.exception.SolicitudInvalidaException;
import pe.edu.upeu.megafarma.repository.VentaRepository;
import pe.edu.upeu.megafarma.service.ResumenVentasService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResumenVentasServiceImpl implements ResumenVentasService {

    private static final String[] MESES = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"};
    private static final String[] MESES_LARGO = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio",
            "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final VentaRepository ventaRepository;

    public ResumenVentasServiceImpl(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenVentasDTO dashboard(Periodo periodo) {
        LocalDate hoy = LocalDate.now();
        return switch (periodo) {
            case HOY, DIA -> calcular("HOY", "Hoy, " + hoy.format(DIA), hoy, hoy, "HORA",
                    hoy.minusDays(1), hoy.minusDays(1));
            case SEMANA -> calcular("SEMANA", "Últimos 7 días", hoy.minusDays(6), hoy, "DIA",
                    hoy.minusDays(13), hoy.minusDays(7));
            case MES -> {
                LocalDate inicio = hoy.withDayOfMonth(1);
                LocalDate inicioAnterior = inicio.minusMonths(1);
                LocalDate finAnterior = inicioAnterior.withDayOfMonth(
                        Math.min(hoy.getDayOfMonth(), inicioAnterior.lengthOfMonth()));
                yield calcular("MES", MESES_LARGO[hoy.getMonthValue() - 1] + " " + hoy.getYear() + " (hasta hoy)",
                        inicio, hoy, "DIA", inicioAnterior, finAnterior);
            }
            case ANIO -> calcular("ANIO", "Año " + hoy.getYear() + " (hasta hoy)", hoy.withDayOfYear(1), hoy, "MES",
                    hoy.minusYears(1).withDayOfYear(1), hoy.minusYears(1));
        };
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenVentasDTO reporte(Periodo tipo, LocalDate fecha) {
        LocalDate f = fecha == null ? LocalDate.now() : fecha;
        return switch (tipo) {
            case DIA, HOY -> calcular("DIA", "Día " + f.format(DIA), f, f, "HORA", f.minusDays(1), f.minusDays(1));
            case MES -> {
                LocalDate inicio = f.withDayOfMonth(1);
                LocalDate fin = f.with(TemporalAdjusters.lastDayOfMonth());
                LocalDate inicioAnterior = inicio.minusMonths(1);
                yield calcular("MES", MESES_LARGO[f.getMonthValue() - 1] + " " + f.getYear(), inicio, fin, "DIA",
                        inicioAnterior, inicioAnterior.with(TemporalAdjusters.lastDayOfMonth()));
            }
            case ANIO -> calcular("ANIO", "Año " + f.getYear(), f.withDayOfYear(1),
                    f.with(TemporalAdjusters.lastDayOfYear()), "MES",
                    f.minusYears(1).withDayOfYear(1), f.minusYears(1).with(TemporalAdjusters.lastDayOfYear()));
            case SEMANA -> throw new SolicitudInvalidaException("Para reportes use DIA, MES o ANIO");
        };
    }

    private ResumenVentasDTO calcular(String periodo, String titulo, LocalDate desde, LocalDate hasta, String agrupacion,
                                      LocalDate desdeAnterior, LocalDate hastaAnterior) {

        List<Venta> todas = ventaRepository.todasPorRango(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
        List<Venta> vigentes = todas.stream().filter(v -> v.getEstado() == EstadoVenta.REGISTRADA).toList();
        List<Venta> anuladas = todas.stream().filter(v -> v.getEstado() == EstadoVenta.ANULADA).toList();

        BigDecimal total = sumar(vigentes);
        int cantidad = vigentes.size();
        BigDecimal ticket = cantidad == 0 ? BigDecimal.ZERO.setScale(2)
                : total.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
        long unidades = vigentes.stream().flatMap(v -> v.getDetalles().stream()).mapToLong(DetalleVenta::getCantidad).sum();

        List<Venta> anteriores = ventaRepository
                .todasPorRango(desdeAnterior.atStartOfDay(), hastaAnterior.plusDays(1).atStartOfDay())
                .stream().filter(v -> v.getEstado() == EstadoVenta.REGISTRADA).toList();
        BigDecimal totalAnterior = sumar(anteriores);
        Double variacion = totalAnterior.signum() == 0 ? null
                : total.subtract(totalAnterior).multiply(BigDecimal.valueOf(100))
                .divide(totalAnterior, 1, RoundingMode.HALF_UP).doubleValue();

        return new ResumenVentasDTO(
                periodo, titulo, desde, hasta, agrupacion,
                total, cantidad, ticket, unidades,
                anuladas.size(), sumar(anuladas),
                totalAnterior, variacion,
                serie(vigentes, desde, hasta, agrupacion),
                topProductos(vigentes),
                porCategoria(vigentes),
                porMedioPago(vigentes),
                contarComprobantes(vigentes, TipoComprobante.BOLETA),
                contarComprobantes(vigentes, TipoComprobante.FACTURA),
                porVendedor(vigentes),
                serie(anteriores, desdeAnterior, hastaAnterior, agrupacion));
    }

    private static int contarComprobantes(List<Venta> ventas, TipoComprobante tipo) {
        return (int) ventas.stream().filter(v -> v.getTipoComprobante() == tipo).count();
    }

    /* Cantidad de ventas e importe de cada vendedor, de mayor a menor. */
    private List<ReporteVentasDTO.Grupo> porVendedor(List<Venta> ventas) {
        Map<String, ReporteVentasDTO.Grupo> grupos = new LinkedHashMap<>();
        for (Venta v : ventas) {
            String nombre = v.getUsuario().getNombreCompleto() == null
                    ? v.getUsuario().getUsername() : v.getUsuario().getNombreCompleto();
            sumarEn(grupos.computeIfAbsent(nombre, this::grupo), v);
        }
        return ordenarPorTotal(grupos);
    }

    /* Serie del grafico con todos los tramos del periodo (los vacios en cero). */
    private List<ReporteVentasDTO.Grupo> serie(List<Venta> ventas, LocalDate desde, LocalDate hasta, String agrupacion) {
        Map<String, ReporteVentasDTO.Grupo> tramos = new LinkedHashMap<>();
        switch (agrupacion) {
            case "HORA" -> {
                for (int h = 0; h < 24; h++) {
                    tramos.put(String.valueOf(h), grupo(String.format("%02d:00", h)));
                }
                ventas.forEach(v -> sumarEn(tramos.get(String.valueOf(v.getFecha().getHour())), v));
            }
            case "DIA" -> {
                DateTimeFormatter corto = DateTimeFormatter.ofPattern("dd/MM");
                for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
                    tramos.put(d.toString(), grupo(d.format(corto)));
                }
                ventas.forEach(v -> sumarEn(tramos.get(v.getFecha().toLocalDate().toString()), v));
            }
            default -> {
                for (LocalDate d = desde.withDayOfMonth(1); !d.isAfter(hasta); d = d.plusMonths(1)) {
                    tramos.put(d.getYear() + "-" + d.getMonthValue(), grupo(MESES[d.getMonthValue() - 1]));
                }
                ventas.forEach(v -> sumarEn(tramos.get(v.getFecha().getYear() + "-" + v.getFecha().getMonthValue()), v));
            }
        }
        return new ArrayList<>(tramos.values());
    }

    private List<ProductoMasVendidoDTO> topProductos(List<Venta> ventas) {
        Map<Long, ProductoMasVendidoDTO> porProducto = new LinkedHashMap<>();
        for (Venta v : ventas) {
            for (DetalleVenta d : v.getDetalles()) {
                Producto p = d.getProducto();
                ProductoMasVendidoDTO fila = porProducto.computeIfAbsent(p.getId(),
                        id -> new ProductoMasVendidoDTO(p.getId(), p.getCodigo(), p.getNombre(), 0, BigDecimal.ZERO));
                fila.setUnidadesVendidas(fila.getUnidadesVendidas() + d.getCantidad());
                fila.setImporte(fila.getImporte().add(d.getSubtotal()));
            }
        }
        return porProducto.values().stream()
                .sorted(Comparator.comparingLong(ProductoMasVendidoDTO::getUnidadesVendidas).reversed()
                        .thenComparing(ProductoMasVendidoDTO::getNombre))
                .limit(5)
                .toList();
    }

    private List<ReporteVentasDTO.Grupo> porCategoria(List<Venta> ventas) {
        Map<String, ReporteVentasDTO.Grupo> grupos = new LinkedHashMap<>();
        for (Venta v : ventas) {
            for (DetalleVenta d : v.getDetalles()) {
                String categoria = d.getProducto().getCategoria().getNombre();
                ReporteVentasDTO.Grupo g = grupos.computeIfAbsent(categoria, this::grupo);
                g.setCantidadVentas(g.getCantidadVentas() + d.getCantidad());
                g.setTotal(g.getTotal().add(d.getSubtotal()));
            }
        }
        return ordenarPorTotal(grupos);
    }

    /* Cada forma de pago suma por separado: un pago mixto reparte la venta. */
    private List<ReporteVentasDTO.Grupo> porMedioPago(List<Venta> ventas) {
        Map<String, ReporteVentasDTO.Grupo> grupos = new LinkedHashMap<>();
        for (Venta v : ventas) {
            if (v.getPagos().isEmpty()) {
                ReporteVentasDTO.Grupo g = grupos.computeIfAbsent(v.getMedioPago().name(), this::grupo);
                g.setCantidadVentas(g.getCantidadVentas() + 1);
                g.setTotal(g.getTotal().add(v.getTotal()));
                continue;
            }
            for (Pago p : v.getPagos()) {
                ReporteVentasDTO.Grupo g = grupos.computeIfAbsent(p.getMedio().name(), this::grupo);
                g.setCantidadVentas(g.getCantidadVentas() + 1);
                g.setTotal(g.getTotal().add(p.getMonto()));
            }
        }
        return ordenarPorTotal(grupos);
    }

    private List<ReporteVentasDTO.Grupo> ordenarPorTotal(Map<String, ReporteVentasDTO.Grupo> grupos) {
        return grupos.values().stream()
                .sorted(Comparator.comparing(ReporteVentasDTO.Grupo::getTotal).reversed())
                .toList();
    }

    private ReporteVentasDTO.Grupo grupo(String etiqueta) {
        return new ReporteVentasDTO.Grupo(etiqueta, 0, BigDecimal.ZERO.setScale(2));
    }

    private void sumarEn(ReporteVentasDTO.Grupo grupo, Venta venta) {
        if (grupo == null) {
            return;
        }
        grupo.setCantidadVentas(grupo.getCantidadVentas() + 1);
        grupo.setTotal(grupo.getTotal().add(venta.getTotal()));
    }

    private static BigDecimal sumar(List<Venta> ventas) {
        return ventas.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }
}
