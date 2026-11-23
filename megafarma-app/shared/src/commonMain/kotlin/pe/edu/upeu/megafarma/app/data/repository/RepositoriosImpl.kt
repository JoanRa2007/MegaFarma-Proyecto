package pe.edu.upeu.megafarma.app.data.repository

import kotlinx.coroutines.flow.StateFlow
import pe.edu.upeu.megafarma.app.core.Resultado
import pe.edu.upeu.megafarma.app.core.mapear
import pe.edu.upeu.megafarma.app.data.local.SesionManager
import pe.edu.upeu.megafarma.app.data.remote.MegafarmaApi
import pe.edu.upeu.megafarma.app.data.remote.dto.EntradaRequestDto
import pe.edu.upeu.megafarma.app.data.remote.dto.LineaVentaDto
import pe.edu.upeu.megafarma.app.data.remote.dto.LoginRequestDto
import pe.edu.upeu.megafarma.app.data.remote.dto.ProductoRequestDto
import pe.edu.upeu.megafarma.app.data.remote.dto.VentaRequestDto
import pe.edu.upeu.megafarma.app.data.remote.dto.aDominio
import pe.edu.upeu.megafarma.app.domain.model.Categoria
import pe.edu.upeu.megafarma.app.domain.model.DatosProducto
import pe.edu.upeu.megafarma.app.domain.model.LineaVenta
import pe.edu.upeu.megafarma.app.domain.model.Lote
import pe.edu.upeu.megafarma.app.domain.model.MedioPago
import pe.edu.upeu.megafarma.app.domain.model.NuevaEntrada
import pe.edu.upeu.megafarma.app.domain.model.PaginaVentas
import pe.edu.upeu.megafarma.app.domain.model.Producto
import pe.edu.upeu.megafarma.app.domain.model.ProductoVendido
import pe.edu.upeu.megafarma.app.domain.model.ReporteInventario
import pe.edu.upeu.megafarma.app.domain.model.ReporteVentas
import pe.edu.upeu.megafarma.app.domain.model.ResumenAlertas
import pe.edu.upeu.megafarma.app.domain.model.Sesion
import pe.edu.upeu.megafarma.app.domain.model.StockProducto
import pe.edu.upeu.megafarma.app.domain.model.Venta
import pe.edu.upeu.megafarma.app.domain.repository.AuthRepository
import pe.edu.upeu.megafarma.app.domain.repository.InventarioRepository
import pe.edu.upeu.megafarma.app.domain.repository.ProductoRepository
import pe.edu.upeu.megafarma.app.domain.repository.ReporteRepository
import pe.edu.upeu.megafarma.app.domain.repository.VentaRepository

/* Implementaciones de los repositorios: llaman a la API y convierten DTO -> dominio. */

class AuthRepositoryImpl(
    private val api: MegafarmaApi,
    private val sesionManager: SesionManager
) : AuthRepository {

    override val sesion: StateFlow<Sesion?> = sesionManager.sesion
    override val servidor: String get() = sesionManager.servidor

    override suspend fun iniciarSesion(usuario: String, clave: String, servidor: String): Resultado<Sesion> {
        val resultado = api.login(servidor, LoginRequestDto(usuario, clave)).mapear { it.aDominio() }
        if (resultado is Resultado.Exito) {
            sesionManager.cambiarServidor(servidor)
            sesionManager.iniciar(resultado.datos)
        }
        return resultado
    }

    /* Primero revoca el token en el servidor (si no hay red, igual cierra la sesion local). */
    override suspend fun cerrarSesion() {
        if (sesionManager.token != null) api.logout()
        sesionManager.cerrar()
    }

    override suspend fun perfil() = api.perfil().mapear { it.aDominio() }

    override suspend fun cambiarPassword(actual: String, nueva: String): Resultado<Unit> {
        val r = api.cambiarPassword(pe.edu.upeu.megafarma.app.data.remote.dto.CambioPasswordDto(actual, nueva))
        if (r is Resultado.Exito) {
            sesionManager.sesion.value?.let { sesionManager.iniciar(it.copy(debeCambiarPassword = false)) }
        }
        return r
    }
}

class ProductoRepositoryImpl(private val api: MegafarmaApi) : ProductoRepository {

    override suspend fun buscar(texto: String, incluirInactivos: Boolean): Resultado<List<Producto>> =
        api.productos(texto, incluirInactivos).mapear { pagina -> pagina.contenido.map { it.aDominio() } }

    override suspend fun categorias(): Resultado<List<Categoria>> =
        api.categorias().mapear { lista -> lista.map { it.aDominio() } }

    override suspend fun guardar(id: Long?, datos: DatosProducto): Resultado<Producto> {
        val request = ProductoRequestDto(
            datos.codigo, datos.nombre, datos.principioActivo, datos.presentacion, datos.laboratorio,
            datos.precioVenta, datos.stockMinimo, datos.requiereReceta, datos.categoriaId
        )
        val resultado = if (id == null) api.crearProducto(request) else api.actualizarProducto(id, request)
        return resultado.mapear { it.aDominio() }
    }

    override suspend fun desactivar(id: Long): Resultado<Unit> = api.desactivarProducto(id)

    override suspend fun activar(id: Long): Resultado<Producto> = api.activarProducto(id).mapear { it.aDominio() }
}

class InventarioRepositoryImpl(private val api: MegafarmaApi) : InventarioRepository {

    override suspend fun stock(productoId: Long): Resultado<StockProducto> =
        api.stock(productoId).mapear { it.aDominio() }

    override suspend fun registrarEntrada(entrada: NuevaEntrada, clave: String): Resultado<Lote> =
        api.registrarEntrada(
            EntradaRequestDto(
                entrada.productoId, entrada.numeroLote.trim(), entrada.fechaVencimiento.trim(),
                entrada.cantidad, entrada.precioCompra, clave
            )
        ).mapear { it.aDominio() }

    override suspend fun darDeBaja(loteId: Long, motivo: String): Resultado<Lote> =
        api.darDeBaja(loteId, motivo).mapear { it.aDominio() }

    override suspend fun resumenAlertas(): Resultado<ResumenAlertas> =
        api.resumenAlertas().mapear { it.aDominio() }

    override suspend fun stockMinimo(): Resultado<List<StockProducto>> =
        api.stockMinimo().mapear { lista -> lista.map { it.aDominio() } }

    override suspend fun vencimientos(dias: Int?): Resultado<List<Lote>> =
        api.vencimientos(dias).mapear { lista -> lista.map { it.aDominio() } }
}

class VentaRepositoryImpl(private val api: MegafarmaApi) : VentaRepository {

    override suspend fun registrar(venta: pe.edu.upeu.megafarma.app.domain.model.NuevaVenta, clave: String): Resultado<Venta> =
        api.registrarVenta(
            VentaRequestDto(
                detalles = venta.lineas.map { LineaVentaDto(it.productoId, it.cantidad) },
                claveIdempotencia = clave,
                pagos = venta.pagos.map {
                    pe.edu.upeu.megafarma.app.data.remote.dto.PagoDto(it.medio.name, it.monto, it.referencia?.trim()?.ifEmpty { null })
                },
                tipoComprobante = venta.tipoComprobante.name,
                clienteId = venta.cliente?.id
            )
        ).mapear { it.aDominio() }

    override suspend fun comprobante(ventaId: Long) = api.comprobante(ventaId).mapear { it.aDominio() }

    override suspend fun comprobanteXml(ventaId: Long) = api.comprobanteXml(ventaId)

    override suspend fun listar(desde: String?, hasta: String?, pagina: Int, tamanio: Int): Resultado<PaginaVentas> =
        api.ventas(desde, hasta, pagina, tamanio).mapear { p ->
            PaginaVentas(p.contenido.map { it.aDominio() }, p.pagina, p.totalPaginas, p.totalElementos)
        }

    override suspend fun buscar(id: Long): Resultado<Venta> = api.venta(id).mapear { it.aDominio() }

    override suspend fun anular(id: Long, motivo: String): Resultado<Venta> =
        api.anularVenta(id, motivo).mapear { it.aDominio() }
}

class ReporteRepositoryImpl(private val api: MegafarmaApi) : ReporteRepository {

    override suspend fun ventas(): Resultado<ReporteVentas> = api.reporteVentas().mapear { it.aDominio() }

    override suspend fun masVendidos(): Resultado<List<ProductoVendido>> =
        api.masVendidos().mapear { lista -> lista.map { it.aDominio() } }

    override suspend fun inventario(): Resultado<ReporteInventario> = api.reporteInventario().mapear { it.aDominio() }

    override suspend fun dashboard(periodo: pe.edu.upeu.megafarma.app.domain.model.Periodo) =
        api.dashboard(periodo.name).mapear { it.aDominio() }

    override suspend fun resumen(tipo: pe.edu.upeu.megafarma.app.domain.model.Periodo, fecha: String?) =
        api.reporteResumen(tipo.name, fecha).mapear { it.aDominio() }
}

class ClienteRepositoryImpl(private val api: MegafarmaApi) : pe.edu.upeu.megafarma.app.domain.repository.ClienteRepository {

    override suspend fun buscar(texto: String) = api.clientes(texto).mapear { p -> p.contenido.map { it.aDominio() } }

    override suspend fun crear(datos: pe.edu.upeu.megafarma.app.domain.model.DatosCliente) =
        api.crearCliente(
            pe.edu.upeu.megafarma.app.data.remote.dto.ClienteRequestDto(
                datos.tipoDocumento, datos.numeroDocumento.trim(), datos.nombre.trim(), datos.telefono?.trim()?.ifEmpty { null }
            )
        ).mapear { it.aDominio() }
}

class UsuarioRepositoryImpl(private val api: MegafarmaApi) : pe.edu.upeu.megafarma.app.domain.repository.UsuarioRepository {

    override suspend fun listar() = api.usuarios().mapear { lista -> lista.map { it.aDominio() } }

    override suspend fun roles() = api.roles().mapear { lista -> lista.map { it.aDominio() } }

    override suspend fun guardar(id: Long?, datos: pe.edu.upeu.megafarma.app.domain.model.DatosUsuario): Resultado<pe.edu.upeu.megafarma.app.domain.model.Usuario> {
        val request = pe.edu.upeu.megafarma.app.data.remote.dto.UsuarioRequestDto(
            username = datos.username.trim(),
            nombres = datos.nombres.trim(),
            apellidos = datos.apellidos.trim(),
            dni = datos.dni.trim().ifBlank { null },
            telefono = datos.telefono.trim().ifBlank { null },
            correo = datos.correo.trim().ifBlank { null },
            rol = datos.rol
        )
        val r = if (id == null) api.crearUsuario(request) else api.actualizarUsuario(id, request)
        return r.mapear { it.aDominio() }
    }

    override suspend fun cambiarEstado(id: Long, activo: Boolean) = api.cambiarEstadoUsuario(id, activo).mapear { it.aDominio() }

    override suspend fun restablecerPassword(id: Long) = api.restablecerPassword(id).mapear { it.aDominio() }

    override suspend fun bitacora(usuario: String, accion: String, pagina: Int) =
        api.auditoria(usuario, accion, pagina).mapear { p ->
            pe.edu.upeu.megafarma.app.domain.model.PaginaAuditoria(p.contenido.map { it.aDominio() }, p.pagina, p.totalPaginas, p.totalElementos)
        }
}
