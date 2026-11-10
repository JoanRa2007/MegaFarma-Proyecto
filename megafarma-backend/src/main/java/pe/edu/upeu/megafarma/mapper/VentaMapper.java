package pe.edu.upeu.megafarma.mapper;

/*
 * PARA ESTUDIAR · MAPPER
 * Convierte entre entidades (base de datos) y DTO (JSON de la API).
 */

import pe.edu.upeu.megafarma.dto.venta.DetalleVentaResponseDTO;
import pe.edu.upeu.megafarma.dto.venta.VentaResponseDTO;
import pe.edu.upeu.megafarma.entity.DetalleVenta;
import pe.edu.upeu.megafarma.entity.Venta;

public final class VentaMapper {

    private VentaMapper() {
    }

    public static VentaResponseDTO toResponse(Venta venta) {
        return new VentaResponseDTO(
                venta.getId(),
                venta.getNumero(),
                venta.getFecha(),
                venta.getEstado().name(),
                venta.getMedioPago().name(),
                venta.getCliente() == null ? null : venta.getCliente().getId(),
                venta.getCliente() == null ? null : venta.getCliente().getNombre(),
                venta.getUsuario().getUsername(),
                venta.getTotal(),
                venta.getMotivoAnulacion(),
                venta.getAnuladaEn(),
                // stream: recorre la coleccion aplicando map/filter y junta el resultado
                venta.getDetalles().stream().map(VentaMapper::toResponse).toList());
    }

    public static DetalleVentaResponseDTO toResponse(DetalleVenta detalle) {
        return new DetalleVentaResponseDTO(
                detalle.getProducto().getId(),
                detalle.getProducto().getCodigo(),
                detalle.getProducto().getNombre(),
                detalle.getLote().getId(),
                detalle.getLote().getNumeroLote(),
                detalle.getLote().getFechaVencimiento(),
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getSubtotal());
    }
}
