package pe.edu.upeu.megafarma.service.impl;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - IMPLEMENTACION (service.impl)
 * Aqui viven las reglas de negocio. El controlador llama al servicio, el servicio
 * valida las reglas, usa los repositorios y devuelve un DTO de respuesta.
 */

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.megafarma.dto.inventario.MovimientoResponseDTO;
import pe.edu.upeu.megafarma.dto.inventario.StockProductoDTO;
import pe.edu.upeu.megafarma.dto.reporte.ProductoMasVendidoDTO;
import pe.edu.upeu.megafarma.dto.reporte.ReporteInventarioDTO;
import pe.edu.upeu.megafarma.dto.reporte.ReporteVentasDTO;
import pe.edu.upeu.megafarma.entity.Producto;
import pe.edu.upeu.megafarma.entity.Venta;
import pe.edu.upeu.megafarma.enums.EstadoVenta;
import pe.edu.upeu.megafarma.enums.TipoMovimiento;
import pe.edu.upeu.megafarma.mapper.InventarioMapper;
import pe.edu.upeu.megafarma.repository.DetalleVentaRepository;
import pe.edu.upeu.megafarma.repository.MovimientoInventarioRepository;
import pe.edu.upeu.megafarma.repository.ProductoRepository;
import pe.edu.upeu.megafarma.repository.VentaRepository;
import pe.edu.upeu.megafarma.service.InventarioService;
import pe.edu.upeu.megafarma.service.ReporteService;
import pe.edu.upeu.megafarma.util.RangoFechas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

// @Service: Spring crea un unico objeto de esta clase y lo inyecta donde se necesite
@Service
// implements: esta clase cumple el contrato (los metodos) de esa interfaz
public class ReporteServiceImpl implements ReporteService {

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final InventarioService inventarioService;

    public ReporteServiceImpl(VentaRepository ventaRepository,
                              DetalleVentaRepository detalleVentaRepository,
                              MovimientoInventarioRepository movimientoRepository,
                              ProductoRepository productoRepository,
                              InventarioService inventarioService) {
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
        this.inventarioService = inventarioService;
    }

    /*
     * HU-17 Reporte de ventas de un rango de fechas: totales, ticket
     * promedio y desglose por dia y por medio de pago. Las ventas anuladas
     * no suman al total vendido; se informan aparte.
     */
    // @Override: este metodo implementa o redefine uno declarado en la interfaz o clase padre
    @Override
    // @Transactional: todo el metodo es una transaccion: si lanza excepcion se deshacen los cambios (rollback)
    @Transactional(readOnly = true)
    public ReporteVentasDTO ventas(LocalDate desde, LocalDate hasta) {

        RangoFechas rango = RangoFechas.de(desde, hasta);
        List<Venta> ventas = ventaRepository.todasPorRango(rango.inicio(), rango.fin());

        int cantidad = 0;
        int anuladas = 0;
        // BigDecimal: decimales exactos para dinero (double pierde centimos por redondeo)
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalAnulado = BigDecimal.ZERO;
        Map<String, ReporteVentasDTO.Grupo> porDia = new TreeMap<>();
        Map<String, ReporteVentasDTO.Grupo> porMedio = new TreeMap<>();

        for (Venta venta : ventas) {
            if (venta.getEstado() == EstadoVenta.ANULADA) {
                anuladas++;
                totalAnulado = totalAnulado.add(venta.getTotal());
                continue;
            }
            cantidad++;
            total = total.add(venta.getTotal());
            acumular(porDia, venta.getFecha().toLocalDate().toString(), venta.getTotal());
            acumular(porMedio, venta.getMedioPago().name(), venta.getTotal());
        }

        BigDecimal ticketPromedio = cantidad == 0
                ? BigDecimal.ZERO.setScale(2)
                : total.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);

        return new ReporteVentasDTO(
                rango.desde(),
                rango.hasta(),
                cantidad,
                total.setScale(2, RoundingMode.HALF_UP),
                ticketPromedio,
                anuladas,
                totalAnulado.setScale(2, RoundingMode.HALF_UP),
                new ArrayList<>(porDia.values()),
                new ArrayList<>(porMedio.values()));
    }

    /* HU-19 Productos mas vendidos por unidades en un rango de fechas. */
    @Override
    // readOnly: transaccion solo de lectura (mas liviana, no guarda cambios)
    @Transactional(readOnly = true)
    public List<ProductoMasVendidoDTO> masVendidos(LocalDate desde, LocalDate hasta, int limite) {

        RangoFechas rango = RangoFechas.de(desde, hasta);
        int limiteSeguro = Math.min(Math.max(limite, 1), 50);

        return detalleVentaRepository
                .masVendidos(EstadoVenta.REGISTRADA, rango.inicio(), rango.fin(),
                        PageRequest.of(0, limiteSeguro))
                // stream: recorre la coleccion aplicando map/filter y junta el resultado
                .stream()
                .map(fila -> new ProductoMasVendidoDTO(
                        (Long) fila[0],
                        (String) fila[1],
                        (String) fila[2],
                        ((Number) fila[3]).longValue(),
                        (BigDecimal) fila[4]))
                .toList();
    }

    /* HU-18 Reporte de inventario: stock disponible valorizado a precio de venta. */
    @Override
    @Transactional(readOnly = true)
    public ReporteInventarioDTO inventario() {

        Map<Long, Producto> productos = productoRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));

        List<ReporteInventarioDTO.Fila> filas = new ArrayList<>();
        int unidades = 0;
        int conStock = 0;
        BigDecimal valorTotal = BigDecimal.ZERO;

        for (StockProductoDTO stock : inventarioService.stockGeneral()) {
            Producto producto = productos.get(stock.getProductoId());
            BigDecimal valor = producto.getPrecioVenta()
                    .multiply(BigDecimal.valueOf(stock.getStockDisponible()))
                    .setScale(2, RoundingMode.HALF_UP);

            filas.add(new ReporteInventarioDTO.Fila(
                    producto.getId(),
                    producto.getCodigo(),
                    producto.getNombre(),
                    producto.getCategoria().getNombre(),
                    stock.getStockDisponible(),
                    stock.getStockMinimo(),
                    stock.isBajoMinimo(),
                    producto.getPrecioVenta(),
                    valor));

            unidades += stock.getStockDisponible();
            valorTotal = valorTotal.add(valor);
            if (stock.getStockDisponible() > 0) {
                conStock++;
            }
        }

        return new ReporteInventarioDTO(conStock, unidades, valorTotal, filas);
    }

    /* HU-20 Reporte de bajas por vencimiento en un rango de fechas. */
    @Override
    @Transactional(readOnly = true)
    public List<MovimientoResponseDTO> bajasPorVencimiento(LocalDate desde, LocalDate hasta) {
        RangoFechas rango = RangoFechas.de(desde, hasta);
        return movimientoRepository
                .porTipoYFecha(TipoMovimiento.BAJA_VENCIMIENTO, rango.inicio(), rango.fin())
                .stream()
                .map(InventarioMapper::toResponse)
                .toList();
    }

    private void acumular(Map<String, ReporteVentasDTO.Grupo> grupos, String etiqueta, BigDecimal monto) {
        ReporteVentasDTO.Grupo grupo = grupos.computeIfAbsent(
                etiqueta, e -> new ReporteVentasDTO.Grupo(e, 0, BigDecimal.ZERO));
        grupo.setCantidadVentas(grupo.getCantidadVentas() + 1);
        grupo.setTotal(grupo.getTotal().add(monto));
    }
}
