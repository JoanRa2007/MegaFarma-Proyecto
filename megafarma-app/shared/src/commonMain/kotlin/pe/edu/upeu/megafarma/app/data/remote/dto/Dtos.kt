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
    val permisos: List<String> = emptyList()
)

@Serializable
data class ErrorDto(val message: String? = null, val validationErrors: Map<String, String>? = null)

@Serializable
data class PaginaDto<T>(
    val contenido: List<T> = emptyList(),
    val totalElementos: Long = 0
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
    val diasParaVencer: Long = 0
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
    val diasConsiderados: Int = 0
)

@Serializable
data class LineaVentaDto(val productoId: Long, val cantidad: Int)

@Serializable
data class VentaRequestDto(
    val medioPago: String,
    val detalles: List<LineaVentaDto>,
    val claveIdempotencia: String,
    val clienteId: Long? = null
)

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
    val detalles: List<DetalleVentaDto> = emptyList()
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
