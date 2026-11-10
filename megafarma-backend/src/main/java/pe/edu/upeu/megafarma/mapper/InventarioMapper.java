package pe.edu.upeu.megafarma.mapper;

/*
 * PARA ESTUDIAR · MAPPER
 * Convierte entre entidades (base de datos) y DTO (JSON de la API).
 */

import pe.edu.upeu.megafarma.dto.inventario.LoteResponseDTO;
import pe.edu.upeu.megafarma.dto.inventario.MovimientoResponseDTO;
import pe.edu.upeu.megafarma.dto.inventario.StockProductoDTO;
import pe.edu.upeu.megafarma.entity.Lote;
import pe.edu.upeu.megafarma.entity.MovimientoInventario;
import pe.edu.upeu.megafarma.entity.Producto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class InventarioMapper {

    private InventarioMapper() {
    }

    public static LoteResponseDTO toResponse(Lote lote, LocalDate hoy) {
        Producto producto = lote.getProducto();
        return new LoteResponseDTO(
                lote.getId(),
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                lote.getNumeroLote(),
                lote.getFechaVencimiento(),
                lote.getCantidadInicial(),
                lote.getCantidadActual(),
                lote.getPrecioCompra(),
                lote.isDadoDeBaja(),
                lote.estaVencido(hoy),
                ChronoUnit.DAYS.between(hoy, lote.getFechaVencimiento()));
    }

    /*
     * Stock de un producto a partir de sus lotes.
     * Registrado = todo lo no dado de baja. Disponible = registrado sin vencidos.
     * Se alerta cuando el disponible llega al stock minimo o baja de el.
     */
    public static StockProductoDTO toStock(Producto producto, List<Lote> lotes, LocalDate hoy) {
        int registrado = 0;
        int disponible = 0;
        for (Lote lote : lotes) {
            if (lote.isDadoDeBaja()) {
                continue;
            }
            registrado += lote.getCantidadActual();
            if (!lote.estaVencido(hoy)) {
                disponible += lote.getCantidadActual();
            }
        }
        return new StockProductoDTO(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                registrado,
                disponible,
                producto.getStockMinimo(),
                disponible <= producto.getStockMinimo(),
                // stream: recorre la coleccion aplicando map/filter y junta el resultado
                lotes.stream().map(l -> toResponse(l, hoy)).toList());
    }

    public static MovimientoResponseDTO toResponse(MovimientoInventario m) {
        return new MovimientoResponseDTO(
                m.getId(),
                m.getFecha(),
                m.getTipo().name(),
                m.getProducto().getId(),
                m.getProducto().getNombre(),
                m.getLote().getId(),
                m.getLote().getNumeroLote(),
                m.getCantidad(),
                m.getStockResultante(),
                m.getMotivo(),
                m.getReferencia(),
                m.getUsuario().getUsername());
    }
}
