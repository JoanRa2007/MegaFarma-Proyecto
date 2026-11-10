package pe.edu.upeu.megafarma.mapper;

/*
 * PARA ESTUDIAR · MAPPER
 * Convierte entre entidades (base de datos) y DTO (JSON de la API).
 */

import pe.edu.upeu.megafarma.dto.ProductoRequestDTO;
import pe.edu.upeu.megafarma.dto.ProductoResponseDTO;
import pe.edu.upeu.megafarma.entity.Categoria;
import pe.edu.upeu.megafarma.entity.Producto;

public final class ProductoMapper {

    private ProductoMapper() {
    }

    public static ProductoResponseDTO toResponse(Producto producto, int stockDisponible) {
        return new ProductoResponseDTO(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getPrincipioActivo(),
                producto.getPresentacion(),
                producto.getLaboratorio(),
                producto.getPrecioVenta(),
                producto.getStockMinimo(),
                producto.isRequiereReceta(),
                producto.isActivo(),
                producto.getCategoria().getId(),
                producto.getCategoria().getNombre(),
                stockDisponible,
                producto.getActualizadoEn());
    }

    /* Copia los datos editables del request a la entidad (crear y actualizar). */
    public static void copiar(ProductoRequestDTO dto, Producto producto, Categoria categoria) {
        producto.setCodigo(dto.getCodigo().trim().toUpperCase());
        producto.setNombre(dto.getNombre().trim());
        producto.setPrincipioActivo(limpiar(dto.getPrincipioActivo()));
        producto.setPresentacion(limpiar(dto.getPresentacion()));
        producto.setLaboratorio(limpiar(dto.getLaboratorio()));
        producto.setPrecioVenta(dto.getPrecioVenta());
        producto.setStockMinimo(dto.getStockMinimo());
        producto.setRequiereReceta(dto.isRequiereReceta());
        producto.setCategoria(categoria);
    }

    private static String limpiar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
