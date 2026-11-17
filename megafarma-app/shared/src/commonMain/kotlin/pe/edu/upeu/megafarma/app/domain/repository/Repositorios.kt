package pe.edu.upeu.megafarma.app.domain.repository

import kotlinx.coroutines.flow.StateFlow
import pe.edu.upeu.megafarma.app.core.Resultado
import pe.edu.upeu.megafarma.app.domain.model.Categoria
import pe.edu.upeu.megafarma.app.domain.model.DatosProducto
import pe.edu.upeu.megafarma.app.domain.model.LineaVenta
import pe.edu.upeu.megafarma.app.domain.model.Lote
import pe.edu.upeu.megafarma.app.domain.model.MedioPago
import pe.edu.upeu.megafarma.app.domain.model.NuevaEntrada
import pe.edu.upeu.megafarma.app.domain.model.Producto
import pe.edu.upeu.megafarma.app.domain.model.ProductoVendido
import pe.edu.upeu.megafarma.app.domain.model.ReporteInventario
import pe.edu.upeu.megafarma.app.domain.model.ReporteVentas
import pe.edu.upeu.megafarma.app.domain.model.ResumenAlertas
import pe.edu.upeu.megafarma.app.domain.model.Sesion
import pe.edu.upeu.megafarma.app.domain.model.StockProducto
import pe.edu.upeu.megafarma.app.domain.model.Venta

/*
 * Contratos de la capa de datos (Clean Architecture): el dominio y los
 * ViewModels solo conocen estas interfaces; la implementacion con Ktor
 * esta en data/repository.
 */

interface AuthRepository {
    val sesion: StateFlow<Sesion?>
    val servidor: String
    suspend fun iniciarSesion(usuario: String, clave: String, servidor: String): Resultado<Sesion>
    fun cerrarSesion()
}

interface ProductoRepository {
    suspend fun buscar(texto: String, incluirInactivos: Boolean = false): Resultado<List<Producto>>
    suspend fun categorias(): Resultado<List<Categoria>>
    suspend fun guardar(id: Long?, datos: DatosProducto): Resultado<Producto>
    suspend fun desactivar(id: Long): Resultado<Unit>
    suspend fun activar(id: Long): Resultado<Producto>
}

interface InventarioRepository {
    suspend fun stock(productoId: Long): Resultado<StockProducto>
    suspend fun registrarEntrada(entrada: NuevaEntrada, clave: String): Resultado<Lote>
    suspend fun darDeBaja(loteId: Long, motivo: String): Resultado<Lote>
    suspend fun resumenAlertas(): Resultado<ResumenAlertas>
    suspend fun stockMinimo(): Resultado<List<StockProducto>>
    suspend fun vencimientos(dias: Int?): Resultado<List<Lote>>
}

interface VentaRepository {
    suspend fun registrar(lineas: List<LineaVenta>, medioPago: MedioPago, clave: String): Resultado<Venta>
    suspend fun listar(desde: String?, hasta: String?): Resultado<List<Venta>>
    suspend fun anular(id: Long, motivo: String): Resultado<Venta>
}

interface ReporteRepository {
    suspend fun ventas(): Resultado<ReporteVentas>
    suspend fun masVendidos(): Resultado<List<ProductoVendido>>
    suspend fun inventario(): Resultado<ReporteInventario>
}
