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
    val permisos: List<String>,
    /* Entro con una contrasena temporal: debe cambiarla antes de usar el sistema. */
    val debeCambiarPassword: Boolean = false
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
    const val USUARIOS_ADMINISTRAR = "USUARIOS_ADMINISTRAR"
    const val CLIENTES_ESCRIBIR = "CLIENTES_ESCRIBIR"
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
    val diasParaVencer: Long,
    /* CRITICO, MEDIO o BAJO; null si no requiere alerta. */
    val nivelAlerta: String? = null
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
    val diasConsiderados: Int,
    val lotesCriticos: Int = 0,
    val lotesMedios: Int = 0,
    val lotesBajos: Int = 0
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
    val detalles: List<DetalleVenta>,
    val subtotal: Double? = null,
    val igv: Double? = null,
    val montoRecibido: Double? = null,
    val vuelto: Double? = null,
    val pagos: List<PagoVenta> = emptyList(),
    val tipoComprobante: String? = null,
    val numeroComprobante: String? = null
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

// ================= Punto de venta =================

/* Forma de pago registrada en una venta. */
data class PagoVenta(val medio: String, val monto: Double, val referencia: String?)

/* Forma de pago que se envia al registrar la venta. */
data class NuevoPago(val medio: MedioPago, val monto: Double, val referencia: String?) {
    val requiereOperacion: Boolean get() = medio == MedioPago.YAPE || medio == MedioPago.PLIN || medio == MedioPago.TRANSFERENCIA
}

enum class TipoComprobante(val etiqueta: String) {
    BOLETA("Boleta"),
    FACTURA("Factura")
}

data class Cliente(
    val id: Long,
    val tipoDocumento: String,
    val numeroDocumento: String,
    val nombre: String,
    val telefono: String?
)

data class DatosCliente(val tipoDocumento: String, val numeroDocumento: String, val nombre: String, val telefono: String?)

/* Todo lo que se necesita para registrar una venta. */
data class NuevaVenta(
    val lineas: List<LineaVenta>,
    val pagos: List<NuevoPago>,
    val tipoComprobante: TipoComprobante,
    val cliente: Cliente?
)

data class ItemComprobante(
    val codigo: String,
    val descripcion: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val valorVenta: Double,
    val igv: Double,
    val importe: Double
)

/* Boleta o factura electronica (modo demostracion). */
data class Comprobante(
    val tipo: String,
    val numero: String,
    val fechaEmision: String,
    val estado: String,
    val emisorRuc: String,
    val emisorRazonSocial: String,
    val emisorDireccion: String,
    val clienteTipoDoc: String?,
    val clienteNumDoc: String?,
    val clienteNombre: String?,
    val items: List<ItemComprobante>,
    val opGravada: Double,
    val igv: Double,
    val total: Double,
    val totalEnLetras: String,
    val pagos: List<PagoVenta>,
    val montoRecibido: Double?,
    val vuelto: Double?,
    val hash: String,
    val textoQr: String,
    val ventaNumero: String
)

enum class Periodo(val etiqueta: String) {
    HOY("Hoy"),
    SEMANA("Semana"),
    MES("Mes"),
    ANIO("Año"),
    DIA("Día")
}

/* Dashboard y reportes por dia, mes y año. */
data class ResumenVentas(
    val periodo: String,
    val titulo: String,
    val desde: String,
    val hasta: String,
    val agrupacion: String,
    val totalVendido: Double,
    val cantidadVentas: Int,
    val ticketPromedio: Double,
    val unidadesVendidas: Long,
    val ventasAnuladas: Int,
    val totalAnulado: Double,
    val totalPeriodoAnterior: Double,
    val variacionPorcentaje: Double?,
    val serie: List<GrupoReporte>,
    val topProductos: List<ProductoVendido>,
    val porCategoria: List<GrupoReporte>,
    val porMedioPago: List<GrupoReporte>
)

data class Usuario(
    val id: Long,
    val username: String,
    val nombreCompleto: String,
    val rol: String,
    val activo: Boolean,
    val bloqueado: Boolean,
    val nombres: String? = null,
    val apellidos: String? = null,
    val dni: String? = null,
    val telefono: String? = null,
    val correo: String? = null,
    val debeCambiarPassword: Boolean = false,
    /* Solo llega al crear la cuenta o al restablecer la contrasena. */
    val passwordTemporal: String? = null
)

data class Rol(val nombre: String, val descripcion: String?, val permisos: List<String>)

/*
 * Datos para crear o editar un trabajador. La contrasena no va aqui: al crear la
 * genera el sistema y cada usuario la cambia desde Mi perfil.
 */
data class DatosUsuario(
    val username: String,
    val nombres: String,
    val apellidos: String,
    val dni: String,
    val telefono: String,
    val correo: String,
    val rol: String
)

/* Evento de la bitacora de seguridad (auditoria). */
data class RegistroAuditoria(
    val id: Long,
    val fecha: String,
    val usuario: String,
    val accion: String,
    val detalle: String?,
    val ip: String?,
    val exito: Boolean
)

data class PaginaAuditoria(
    val registros: List<RegistroAuditoria>,
    val pagina: Int,
    val totalPaginas: Int,
    val totalElementos: Long
)
