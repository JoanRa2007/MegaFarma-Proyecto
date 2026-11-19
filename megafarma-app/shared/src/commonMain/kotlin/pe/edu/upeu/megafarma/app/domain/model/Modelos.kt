package pe.edu.upeu.megafarma.app.domain.model

/*
 * Modelos del dominio: lo que la app entiende como "producto", "venta", etc.
 * No dependen de Ktor ni del formato JSON (eso es tarea de la capa de datos).
 */

data class Sesion(
    val token: String,
    val username: String,
    val nombreCompleto: String,
    val rol: String,
    val permisos: List<String>
) {
    fun puede(permiso: String): Boolean = permiso in permisos
}

/* Nombres de los permisos tal como los envia el backend. */
object Permiso {
    const val CATALOGO_ESCRIBIR = "CATALOGO_ESCRIBIR"
    const val INVENTARIO_LEER = "INVENTARIO_LEER"
    const val INVENTARIO_ESCRIBIR = "INVENTARIO_ESCRIBIR"
    const val VENTAS_LEER = "VENTAS_LEER"
    const val VENTAS_REGISTRAR = "VENTAS_REGISTRAR"
    const val VENTAS_ANULAR = "VENTAS_ANULAR"
    const val REPORTES_VER = "REPORTES_VER"
}

data class Producto(
    val id: Long,
    val codigo: String,
    val nombre: String,
    val principioActivo: String?,
    val presentacion: String?,
    val laboratorio: String?,
    val precioVenta: Double,
    val stockMinimo: Int,
    val requiereReceta: Boolean,
    val activo: Boolean,
    val categoriaId: Long,
    val categoriaNombre: String,
    val stockDisponible: Int
)

data class Categoria(val id: Long, val nombre: String)

/* Datos que se envian para crear o editar un producto. */
data class DatosProducto(
    val codigo: String,
    val nombre: String,
    val principioActivo: String?,
    val presentacion: String?,
    val laboratorio: String?,
    val precioVenta: Double,
    val stockMinimo: Int,
    val requiereReceta: Boolean,
    val categoriaId: Long
)

data class Lote(
    val id: Long,
    val productoCodigo: String,
    val productoNombre: String,
    val numeroLote: String,
    val fechaVencimiento: String,
    val cantidadInicial: Int,
    val cantidadActual: Int,
    val dadoDeBaja: Boolean,
    val vencido: Boolean,
    val diasParaVencer: Long
)

data class StockProducto(
    val productoId: Long,
    val codigo: String,
    val nombre: String,
    val stockRegistrado: Int,
    val stockDisponible: Int,
    val stockMinimo: Int,
    val bajoMinimo: Boolean,
    val lotes: List<Lote>
)

data class NuevaEntrada(
    val productoId: Long,
    val numeroLote: String,
    val fechaVencimiento: String,
    val cantidad: Int,
    val precioCompra: Double?
)

data class ResumenAlertas(
    val productosBajoMinimo: Int,
    val lotesPorVencer: Int,
    val lotesVencidos: Int,
    val diasConsiderados: Int
)

enum class MedioPago(val etiqueta: String) {
    EFECTIVO("Efectivo"),
    TARJETA("Tarjeta"),
    YAPE("Yape"),
    PLIN("Plin"),
    TRANSFERENCIA("Transferencia")
}

data class LineaVenta(val productoId: Long, val cantidad: Int)

data class DetalleVenta(
    val productoNombre: String,
    val numeroLote: String,
    val fechaVencimiento: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
)

data class Venta(
    val id: Long,
    val numero: String,
    val fecha: String,
    val estado: String,
    val medioPago: String,
    val clienteNombre: String?,
    val vendedor: String,
    val total: Double,
    val motivoAnulacion: String?,
    val detalles: List<DetalleVenta>
) {
    val anulada: Boolean get() = estado == "ANULADA"
}

/* Una pagina de resultados de la busqueda de ventas (HU-11). */
data class PaginaVentas(
    val ventas: List<Venta>,
    val pagina: Int,
    val totalPaginas: Int,
    val totalElementos: Long
)

data class GrupoReporte(val etiqueta: String, val cantidadVentas: Int, val total: Double)

data class ReporteVentas(
    val desde: String,
    val hasta: String,
    val cantidadVentas: Int,
    val totalVendido: Double,
    val ticketPromedio: Double,
    val ventasAnuladas: Int,
    val totalAnulado: Double,
    val porDia: List<GrupoReporte>,
    val porMedioPago: List<GrupoReporte>
)

data class ProductoVendido(val codigo: String, val nombre: String, val unidades: Long, val importe: Double)

data class ReporteInventario(val productosConStock: Int, val unidadesTotales: Int, val valorTotal: Double)
