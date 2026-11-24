package pe.edu.upeu.megafarma.app.data.remote.dto

import pe.edu.upeu.megafarma.app.domain.model.Categoria
import pe.edu.upeu.megafarma.app.domain.model.DetalleVenta
import pe.edu.upeu.megafarma.app.domain.model.GrupoReporte
import pe.edu.upeu.megafarma.app.domain.model.Lote
import pe.edu.upeu.megafarma.app.domain.model.Pagina
import pe.edu.upeu.megafarma.app.domain.model.Producto
import pe.edu.upeu.megafarma.app.domain.model.ProductoVendido
import pe.edu.upeu.megafarma.app.domain.model.ReporteInventario
import pe.edu.upeu.megafarma.app.domain.model.ReporteVentas
import pe.edu.upeu.megafarma.app.domain.model.ResumenAlertas
import pe.edu.upeu.megafarma.app.domain.model.Sesion
import pe.edu.upeu.megafarma.app.domain.model.StockProducto
import pe.edu.upeu.megafarma.app.domain.model.Venta

/* Conversion DTO (JSON) -> modelo de dominio. */

fun <T, R> PaginaDto<T>.aDominio(transformar: (T) -> R) = Pagina(
    contenido.map(transformar), pagina, totalPaginas, totalElementos
)

fun LoginResponseDto.aDominio() = Sesion(token, username, nombreCompleto, rol, permisos, debeCambiarPassword)

fun CategoriaDto.aDominio() = Categoria(id, nombre)

fun ProductoDto.aDominio() = Producto(
    id, codigo, nombre, principioActivo, presentacion, laboratorio, precioVenta,
    stockMinimo, requiereReceta, activo, categoriaId, categoriaNombre, stockDisponible
)

fun LoteDto.aDominio() = Lote(
    id, productoCodigo, productoNombre, numeroLote, fechaVencimiento,
    cantidadInicial, cantidadActual, dadoDeBaja, vencido, diasParaVencer, nivelAlerta
)

fun StockProductoDto.aDominio() = StockProducto(
    productoId, codigo, nombre, stockRegistrado, stockDisponible, stockMinimo, bajoMinimo,
    lotes.map { it.aDominio() }
)

fun ResumenAlertasDto.aDominio() = ResumenAlertas(
    productosBajoMinimo, lotesPorVencer, lotesVencidos, diasConsiderados, lotesCriticos, lotesMedios, lotesBajos
)

fun VentaDto.aDominio() = Venta(
    id, numero ?: "V-$id", fecha, estado, medioPago, clienteNombre, vendedor, total, motivoAnulacion,
    detalles.map { DetalleVenta(it.productoNombre, it.numeroLote, it.fechaVencimiento, it.cantidad, it.precioUnitario, it.subtotal) },
    subtotal, igv, montoRecibido, vuelto,
    pagos.map { it.aDominio() },
    tipoComprobante, numeroComprobante
)

fun GrupoDto.aDominio() = GrupoReporte(etiqueta, cantidadVentas, total)

fun ReporteVentasDto.aDominio() = ReporteVentas(
    desde, hasta, cantidadVentas, totalVendido, ticketPromedio, ventasAnuladas, totalAnulado,
    porDia.map { it.aDominio() }, porMedioPago.map { it.aDominio() }
)

fun MasVendidoDto.aDominio() = ProductoVendido(codigo, nombre, unidadesVendidas, importe)

fun ReporteInventarioDto.aDominio() = ReporteInventario(productosConStock, unidadesTotales, valorAPrecioVenta)

fun PagoDto.aDominio() = pe.edu.upeu.megafarma.app.domain.model.PagoVenta(medio, monto, referencia)

fun ComprobanteDto.aDominio() = pe.edu.upeu.megafarma.app.domain.model.Comprobante(
    tipo, numero, fechaEmision, estado, emisorRuc, emisorRazonSocial, emisorDireccion,
    clienteTipoDoc, clienteNumDoc, clienteNombre,
    items.map {
        pe.edu.upeu.megafarma.app.domain.model.ItemComprobante(
            it.codigo, it.descripcion, it.cantidad, it.precioUnitario, it.valorVenta, it.igv, it.importe
        )
    },
    opGravada, igv, total, totalEnLetras, pagos.map { it.aDominio() }, montoRecibido, vuelto, hash, textoQr, ventaNumero
)

fun ResumenVentasDto.aDominio() = pe.edu.upeu.megafarma.app.domain.model.ResumenVentas(
    periodo, titulo, desde, hasta, agrupacion, totalVendido, cantidadVentas, ticketPromedio, unidadesVendidas,
    ventasAnuladas, totalAnulado, totalPeriodoAnterior, variacionPorcentaje,
    serie.map { it.aDominio() }, topProductos.map { it.aDominio() }, porCategoria.map { it.aDominio() },
    porMedioPago.map { it.aDominio() }, boletasEmitidas, facturasEmitidas,
    porVendedor.map { it.aDominio() }, serieAnterior.map { it.aDominio() },
    serieTabla.aDominio { it.aDominio() }, topProductosTabla.aDominio { it.aDominio() }
)

fun ClienteDto.aDominio() = pe.edu.upeu.megafarma.app.domain.model.Cliente(id, tipoDocumento, numeroDocumento, nombre, telefono)

fun UsuarioDto.aDominio() = pe.edu.upeu.megafarma.app.domain.model.Usuario(
    id, username, nombreCompleto, rol, activo, bloqueado,
    nombres, apellidos, dni, telefono, correo, debeCambiarPassword, passwordTemporal
)

fun RolDto.aDominio() = pe.edu.upeu.megafarma.app.domain.model.Rol(nombre, descripcion, permisos)

fun AuditoriaDto.aDominio() = pe.edu.upeu.megafarma.app.domain.model.RegistroAuditoria(id, fecha, usuario, accion, detalle, ip, exito)
