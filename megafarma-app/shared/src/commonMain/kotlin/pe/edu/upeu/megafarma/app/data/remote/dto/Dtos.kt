package pe.edu.upeu.megafarma.app.data.remote.dto

import kotlinx.serialization.Serializable

/*
 * DTO: forma exacta del JSON que envia y recibe la API.
 * kotlinx.serialization convierte entre JSON y estas clases.
 */

@Serializable
data class LoginRequestDto(val username: String, val password: String)

@Serializable
data class LoginResponseDto(
    val token: String,
    val username: String,
    val nombreCompleto: String,
    val rol: String,
    val permisos: List<String> = emptyList(),
    val debeCambiarPassword: Boolean = false
)

@Serializable
data class CambioPasswordDto(val actual: String, val nueva: String)

@Serializable
data class ErrorDto(val message: String? = null, val validationErrors: Map<String, String>? = null)

@Serializable
data class PaginaDto<T>(
    val contenido: List<T> = emptyList(),
    val pagina: Int = 0,
    val totalPaginas: Int = 0,
    val totalElementos: Long = 0,
    val ultima: Boolean = true
)

@Serializable
data class CategoriaDto(val id: Long, val nombre: String)

@Serializable
data class ProductoDto(
    val id: Long,
    val codigo: String,
    val nombre: String,
    val principioActivo: String? = null,
    val presentacion: String? = null,
    val laboratorio: String? = null,
    val precioVenta: Double,
    val stockMinimo: Int = 0,
    val requiereReceta: Boolean = false,
    val activo: Boolean = true,
    val categoriaId: Long,
    val categoriaNombre: String = "",
    val stockDisponible: Int = 0
)

@Serializable
data class ProductoRequestDto(
    val codigo: String,
    val nombre: String,
    val principioActivo: String? = null,
    val presentacion: String? = null,
    val laboratorio: String? = null,
    val precioVenta: Double,
    val stockMinimo: Int,
    val requiereReceta: Boolean,
    val categoriaId: Long
)

@Serializable
data class LoteDto(
    val id: Long,
    val productoCodigo: String = "",
    val productoNombre: String = "",
    val numeroLote: String,
    val fechaVencimiento: String,
    val cantidadInicial: Int = 0,
    val cantidadActual: Int = 0,
    val dadoDeBaja: Boolean = false,
    val vencido: Boolean = false,
    val diasParaVencer: Long = 0,
    val nivelAlerta: String? = null
)

@Serializable
data class StockProductoDto(
    val productoId: Long,
    val codigo: String,
    val nombre: String,
    val stockRegistrado: Int = 0,
    val stockDisponible: Int = 0,
    val stockMinimo: Int = 0,
    val bajoMinimo: Boolean = false,
    val lotes: List<LoteDto> = emptyList()
)

@Serializable
data class EntradaRequestDto(
    val productoId: Long,
    val numeroLote: String,
    val fechaVencimiento: String,
    val cantidad: Int,
    val precioCompra: Double? = null,
    val claveIdempotencia: String
)

@Serializable
data class MotivoDto(val motivo: String)

@Serializable
data class ResumenAlertasDto(
    val productosBajoMinimo: Int = 0,
    val lotesPorVencer: Int = 0,
    val lotesVencidos: Int = 0,
    val diasConsiderados: Int = 0,
    val lotesCriticos: Int = 0,
    val lotesMedios: Int = 0,
    val lotesBajos: Int = 0
)

@Serializable
data class LineaVentaDto(val productoId: Long, val cantidad: Int)

@Serializable
data class VentaRequestDto(
    val detalles: List<LineaVentaDto>,
    val claveIdempotencia: String,
    val pagos: List<PagoDto>,
    val tipoComprobante: String,
    val clienteId: Long? = null,
    val medioPago: String? = null
)

@Serializable
data class PagoDto(val medio: String, val monto: Double, val referencia: String? = null)

@Serializable
data class DetalleVentaDto(
    val productoNombre: String,
    val numeroLote: String,
    val fechaVencimiento: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
)

@Serializable
data class VentaDto(
    val id: Long,
    val numero: String? = null,
    val fecha: String,
    val estado: String,
    val medioPago: String,
    val clienteNombre: String? = null,
    val vendedor: String = "",
    val total: Double,
    val motivoAnulacion: String? = null,
    val detalles: List<DetalleVentaDto> = emptyList(),
    val subtotal: Double? = null,
    val igv: Double? = null,
    val montoRecibido: Double? = null,
    val vuelto: Double? = null,
    val pagos: List<PagoDto> = emptyList(),
    val tipoComprobante: String? = null,
    val numeroComprobante: String? = null
)

@Serializable
data class GrupoDto(val etiqueta: String, val cantidadVentas: Int, val total: Double)

@Serializable
data class ReporteVentasDto(
    val desde: String,
    val hasta: String,
    val cantidadVentas: Int = 0,
    val totalVendido: Double = 0.0,
    val ticketPromedio: Double = 0.0,
    val ventasAnuladas: Int = 0,
    val totalAnulado: Double = 0.0,
    val porDia: List<GrupoDto> = emptyList(),
    val porMedioPago: List<GrupoDto> = emptyList()
)

@Serializable
data class MasVendidoDto(val codigo: String, val nombre: String, val unidadesVendidas: Long, val importe: Double)

@Serializable
data class ReporteInventarioDto(
    val productosConStock: Int = 0,
    val unidadesTotales: Int = 0,
    val valorAPrecioVenta: Double = 0.0
)

@Serializable
data class ItemComprobanteDto(
    val codigo: String,
    val descripcion: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val valorVenta: Double,
    val igv: Double,
    val importe: Double
)

@Serializable
data class ComprobanteDto(
    val tipo: String,
    val numero: String,
    val fechaEmision: String,
    val estado: String,
    val emisorRuc: String,
    val emisorRazonSocial: String,
    val emisorDireccion: String,
    val clienteTipoDoc: String? = null,
    val clienteNumDoc: String? = null,
    val clienteNombre: String? = null,
    val items: List<ItemComprobanteDto> = emptyList(),
    val opGravada: Double,
    val igv: Double,
    val total: Double,
    val totalEnLetras: String = "",
    val pagos: List<PagoDto> = emptyList(),
    val montoRecibido: Double? = null,
    val vuelto: Double? = null,
    val hash: String = "",
    val textoQr: String = "",
    val ventaNumero: String = ""
)

@Serializable
data class ResumenVentasDto(
    val periodo: String,
    val titulo: String,
    val desde: String,
    val hasta: String,
    val agrupacion: String,
    val totalVendido: Double = 0.0,
    val cantidadVentas: Int = 0,
    val ticketPromedio: Double = 0.0,
    val unidadesVendidas: Long = 0,
    val ventasAnuladas: Int = 0,
    val totalAnulado: Double = 0.0,
    val totalPeriodoAnterior: Double = 0.0,
    val variacionPorcentaje: Double? = null,
    val serie: List<GrupoDto> = emptyList(),
    val topProductos: List<MasVendidoDto> = emptyList(),
    val porCategoria: List<GrupoDto> = emptyList(),
    val porMedioPago: List<GrupoDto> = emptyList(),
    val boletasEmitidas: Int = 0,
    val facturasEmitidas: Int = 0,
    val porVendedor: List<GrupoDto> = emptyList(),
    val serieAnterior: List<GrupoDto> = emptyList()
)

@Serializable
data class ClienteDto(
    val id: Long,
    val tipoDocumento: String,
    val numeroDocumento: String,
    val nombre: String,
    val telefono: String? = null
)

@Serializable
data class ClienteRequestDto(val tipoDocumento: String, val numeroDocumento: String, val nombre: String, val telefono: String? = null)

@Serializable
data class UsuarioDto(
    val id: Long,
    val username: String,
    val nombreCompleto: String,
    val rol: String,
    val activo: Boolean = true,
    val bloqueado: Boolean = false,
    val nombres: String? = null,
    val apellidos: String? = null,
    val dni: String? = null,
    val telefono: String? = null,
    val correo: String? = null,
    val debeCambiarPassword: Boolean = false,
    val passwordTemporal: String? = null
)

@Serializable
data class UsuarioRequestDto(
    val username: String,
    val nombres: String,
    val apellidos: String,
    val dni: String? = null,
    val telefono: String? = null,
    val correo: String? = null,
    val rol: String
)

@Serializable
data class RolDto(val nombre: String, val descripcion: String? = null, val permisos: List<String> = emptyList())

@Serializable
data class AuditoriaDto(
    val id: Long,
    val fecha: String,
    val usuario: String,
    val accion: String,
    val detalle: String? = null,
    val ip: String? = null,
    val exito: Boolean = true
)
