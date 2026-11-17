package pe.edu.upeu.megafarma.app.data.remote.dto

import pe.edu.upeu.megafarma.app.domain.model.Categoria
import pe.edu.upeu.megafarma.app.domain.model.DetalleVenta
import pe.edu.upeu.megafarma.app.domain.model.GrupoReporte
import pe.edu.upeu.megafarma.app.domain.model.Lote
import pe.edu.upeu.megafarma.app.domain.model.Producto
import pe.edu.upeu.megafarma.app.domain.model.ProductoVendido
import pe.edu.upeu.megafarma.app.domain.model.ReporteInventario
import pe.edu.upeu.megafarma.app.domain.model.ReporteVentas
import pe.edu.upeu.megafarma.app.domain.model.ResumenAlertas
import pe.edu.upeu.megafarma.app.domain.model.Sesion
import pe.edu.upeu.megafarma.app.domain.model.StockProducto
import pe.edu.upeu.megafarma.app.domain.model.Venta

/* Conversion DTO (JSON) -> modelo de dominio. */

fun LoginResponseDto.aDominio() = Sesion(token, username, nombreCompleto, rol, permisos)

fun CategoriaDto.aDominio() = Categoria(id, nombre)

fun ProductoDto.aDominio() = Producto(
    id, codigo, nombre, principioActivo, presentacion, laboratorio, precioVenta,
    stockMinimo, requiereReceta, activo, categoriaId, categoriaNombre, stockDisponible
)

fun LoteDto.aDominio() = Lote(
    id, productoCodigo, productoNombre, numeroLote, fechaVencimiento,
    cantidadInicial, cantidadActual, dadoDeBaja, vencido, diasParaVencer
)

fun StockProductoDto.aDominio() = StockProducto(
    productoId, codigo, nombre, stockRegistrado, stockDisponible, stockMinimo, bajoMinimo,
    lotes.map { it.aDominio() }
)

fun ResumenAlertasDto.aDominio() = ResumenAlertas(productosBajoMinimo, lotesPorVencer, lotesVencidos, diasConsiderados)

fun VentaDto.aDominio() = Venta(
    id, numero ?: "V-$id", fecha, estado, medioPago, clienteNombre, vendedor, total, motivoAnulacion,
    detalles.map { DetalleVenta(it.productoNombre, it.numeroLote, it.fechaVencimiento, it.cantidad, it.precioUnitario, it.subtotal) }
)

fun GrupoDto.aDominio() = GrupoReporte(etiqueta, cantidadVentas, total)

fun ReporteVentasDto.aDominio() = ReporteVentas(
    desde, hasta, cantidadVentas, totalVendido, ticketPromedio, ventasAnuladas, totalAnulado,
    porDia.map { it.aDominio() }, porMedioPago.map { it.aDominio() }
)

fun MasVendidoDto.aDominio() = ProductoVendido(codigo, nombre, unidadesVendidas, importe)

fun ReporteInventarioDto.aDominio() = ReporteInventario(productosConStock, unidadesTotales, valorAPrecioVenta)
