package pe.edu.upeu.megafarma.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import pe.edu.upeu.megafarma.app.core.Resultado
import pe.edu.upeu.megafarma.app.data.local.SesionManager
import pe.edu.upeu.megafarma.app.data.remote.dto.CategoriaDto
import pe.edu.upeu.megafarma.app.data.remote.dto.ClienteDto
import pe.edu.upeu.megafarma.app.data.remote.dto.ClienteRequestDto
import pe.edu.upeu.megafarma.app.data.remote.dto.ComprobanteDto
import pe.edu.upeu.megafarma.app.data.remote.dto.ResumenVentasDto
import pe.edu.upeu.megafarma.app.data.remote.dto.RolDto
import pe.edu.upeu.megafarma.app.data.remote.dto.UsuarioDto
import pe.edu.upeu.megafarma.app.data.remote.dto.UsuarioRequestDto
import pe.edu.upeu.megafarma.app.data.remote.dto.EntradaRequestDto
import pe.edu.upeu.megafarma.app.data.remote.dto.ErrorDto
import pe.edu.upeu.megafarma.app.data.remote.dto.LoginRequestDto
import pe.edu.upeu.megafarma.app.data.remote.dto.LoginResponseDto
import pe.edu.upeu.megafarma.app.data.remote.dto.LoteDto
import pe.edu.upeu.megafarma.app.data.remote.dto.MasVendidoDto
import pe.edu.upeu.megafarma.app.data.remote.dto.MotivoDto
import pe.edu.upeu.megafarma.app.data.remote.dto.PaginaDto
import pe.edu.upeu.megafarma.app.data.remote.dto.ProductoDto
import pe.edu.upeu.megafarma.app.data.remote.dto.ProductoRequestDto
import pe.edu.upeu.megafarma.app.data.remote.dto.ReporteInventarioDto
import pe.edu.upeu.megafarma.app.data.remote.dto.ReporteVentasDto
import pe.edu.upeu.megafarma.app.data.remote.dto.ResumenAlertasDto
import pe.edu.upeu.megafarma.app.data.remote.dto.StockProductoDto
import pe.edu.upeu.megafarma.app.data.remote.dto.VentaDto
import pe.edu.upeu.megafarma.app.data.remote.dto.VentaRequestDto

/*
 * Todas las llamadas HTTP a la API del backend (GET, POST, PUT, PATCH, DELETE).
 * Cada metodo devuelve Resultado: Exito con el JSON ya convertido, o Error con
 * el mensaje que envio el servidor (por ejemplo "Stock insuficiente...").
 */
class MegafarmaApi(
    private val client: HttpClient,
    private val sesion: SesionManager
) {
    private fun url(ruta: String) = "${sesion.servidor}/api/v1$ruta"

    // ---------- Autenticacion ----------
    suspend fun login(servidor: String, request: LoginRequestDto): Resultado<LoginResponseDto> = llamar {
        post("$servidor/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    /* Revoca el token en el servidor: aunque alguien lo hubiera copiado, deja de servir. */
    suspend fun logout(): Resultado<Unit> = llamar { post(url("/auth/logout")) }

    /* Datos del usuario que tiene la sesion abierta (Mi perfil). */
    suspend fun perfil(): Resultado<UsuarioDto> = llamar { get(url("/auth/me")) }

    suspend fun cambiarPassword(request: pe.edu.upeu.megafarma.app.data.remote.dto.CambioPasswordDto): Resultado<Unit> = llamar {
        put(url("/auth/password")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    // ---------- Catalogo ----------
    suspend fun productos(texto: String, incluirInactivos: Boolean): Resultado<PaginaDto<ProductoDto>> = llamar {
        get(url("/productos")) {
            if (texto.isNotBlank()) parameter("q", texto)
            parameter("incluirInactivos", incluirInactivos)
            parameter("tamanio", 100)
        }
    }

    suspend fun categorias(): Resultado<List<CategoriaDto>> = llamar { get(url("/categorias")) }

    suspend fun crearProducto(request: ProductoRequestDto): Resultado<ProductoDto> = llamar {
        post(url("/productos")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun actualizarProducto(id: Long, request: ProductoRequestDto): Resultado<ProductoDto> = llamar {
        put(url("/productos/$id")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun desactivarProducto(id: Long): Resultado<Unit> = llamar { delete(url("/productos/$id")) }

    suspend fun activarProducto(id: Long): Resultado<ProductoDto> = llamar { patch(url("/productos/$id/activar")) }

    // ---------- Inventario y alertas ----------
    suspend fun stock(productoId: Long): Resultado<StockProductoDto> = llamar { get(url("/inventario/stock/$productoId")) }

    suspend fun registrarEntrada(request: EntradaRequestDto): Resultado<LoteDto> = llamar {
        post(url("/inventario/entradas")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun darDeBaja(loteId: Long, motivo: String): Resultado<LoteDto> = llamar {
        post(url("/inventario/lotes/$loteId/baja")) {
            contentType(ContentType.Application.Json)
            setBody(MotivoDto(motivo))
        }
    }

    suspend fun resumenAlertas(): Resultado<ResumenAlertasDto> = llamar { get(url("/alertas/resumen")) }

    suspend fun stockMinimo(): Resultado<List<StockProductoDto>> = llamar { get(url("/alertas/stock-minimo")) }

    suspend fun vencimientos(dias: Int?): Resultado<List<LoteDto>> = llamar {
        get(url("/alertas/vencimientos")) { if (dias != null) parameter("dias", dias) }
    }

    // ---------- Ventas ----------
    suspend fun registrarVenta(request: VentaRequestDto): Resultado<VentaDto> = llamar {
        post(url("/ventas")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun ventas(desde: String?, hasta: String?, pagina: Int, tamanio: Int): Resultado<PaginaDto<VentaDto>> = llamar {
        get(url("/ventas")) {
            if (desde != null) parameter("desde", desde)
            if (hasta != null) parameter("hasta", hasta)
            parameter("pagina", pagina)
            parameter("tamanio", tamanio)
        }
    }

    /* HU-10: detalle actualizado de una venta (por si otra sesion la anulo). */
    suspend fun venta(id: Long): Resultado<VentaDto> = llamar { get(url("/ventas/$id")) }

    suspend fun anularVenta(id: Long, motivo: String): Resultado<VentaDto> = llamar {
        post(url("/ventas/$id/anular")) {
            contentType(ContentType.Application.Json)
            setBody(MotivoDto(motivo))
        }
    }

    suspend fun comprobante(ventaId: Long): Resultado<ComprobanteDto> = llamar { get(url("/ventas/$ventaId/comprobante")) }

    suspend fun comprobanteXml(ventaId: Long): Resultado<String> = llamar { get(url("/ventas/$ventaId/comprobante/xml")) }

    // ---------- Clientes ----------
    suspend fun clientes(texto: String): Resultado<PaginaDto<ClienteDto>> = llamar {
        get(url("/clientes")) {
            if (texto.isNotBlank()) parameter("q", texto)
            parameter("tamanio", 20)
        }
    }

    suspend fun crearCliente(request: ClienteRequestDto): Resultado<ClienteDto> = llamar {
        post(url("/clientes")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    // ---------- Usuarios (solo ADMIN) ----------
    suspend fun usuarios(): Resultado<List<UsuarioDto>> = llamar { get(url("/usuarios")) }

    suspend fun roles(): Resultado<List<RolDto>> = llamar { get(url("/roles")) }

    suspend fun crearUsuario(request: UsuarioRequestDto): Resultado<UsuarioDto> = llamar {
        post(url("/usuarios")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun actualizarUsuario(id: Long, request: UsuarioRequestDto): Resultado<UsuarioDto> = llamar {
        put(url("/usuarios/$id")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun cambiarEstadoUsuario(id: Long, activo: Boolean): Resultado<UsuarioDto> = llamar {
        patch(url("/usuarios/$id/estado")) { parameter("activo", activo) }
    }

    /* Genera una contrasena temporal para el trabajador (recuperar contrasena). */
    suspend fun restablecerPassword(id: Long): Resultado<UsuarioDto> = llamar { post(url("/usuarios/$id/restablecer-password")) }

    /* Bitacora de seguridad (solo ADMIN). */
    suspend fun auditoria(usuario: String, accion: String, pagina: Int): Resultado<PaginaDto<pe.edu.upeu.megafarma.app.data.remote.dto.AuditoriaDto>> = llamar {
        get(url("/auditoria")) {
            if (usuario.isNotBlank()) parameter("usuario", usuario)
            if (accion.isNotBlank()) parameter("accion", accion)
            parameter("pagina", pagina)
            parameter("tamanio", 50)
        }
    }

    // ---------- Dashboard ----------
    suspend fun dashboard(periodo: String): Resultado<ResumenVentasDto> = llamar {
        get(url("/dashboard")) { parameter("periodo", periodo) }
    }

    suspend fun reporteResumen(tipo: String, fecha: String?): Resultado<ResumenVentasDto> = llamar {
        get(url("/reportes/resumen")) {
            parameter("tipo", tipo)
            if (!fecha.isNullOrBlank()) parameter("fecha", fecha)
        }
    }

    // ---------- Reportes ----------
    suspend fun reporteVentas(): Resultado<ReporteVentasDto> = llamar { get(url("/reportes/ventas")) }

    suspend fun masVendidos(): Resultado<List<MasVendidoDto>> = llamar {
        get(url("/reportes/mas-vendidos")) { parameter("limite", 5) }
    }

    suspend fun reporteInventario(): Resultado<ReporteInventarioDto> = llamar { get(url("/reportes/inventario")) }

    /*
     * Ejecuta la peticion y traduce la respuesta:
     *  - 2xx: convierte el JSON al tipo T.
     *  - 401 con sesion abierta: el token vencio, se cierra la sesion (vuelve al login).
     *  - otro error: lee el campo "message" del JSON de error del backend.
     *  - sin conexion: mensaje claro en vez de una excepcion.
     */
    private suspend inline fun <reified T> llamar(crossinline peticion: suspend HttpClient.() -> HttpResponse): Resultado<T> {
        return try {
            val respuesta = client.peticion()
            if (respuesta.status.isSuccess()) {
                @Suppress("UNCHECKED_CAST")
                val datos = if (T::class == Unit::class) Unit as T else respuesta.body<T>()
                Resultado.Exito(datos)
            } else {
                if (respuesta.status == HttpStatusCode.Unauthorized && sesion.token != null) {
                    sesion.cerrar()
                }
                Resultado.Error(mensajeDeError(respuesta), respuesta.status.value)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resultado.Error("No se pudo conectar con el servidor (${sesion.servidor}). Revise que el backend esté encendido.")
        }
    }

    private suspend fun mensajeDeError(respuesta: HttpResponse): String = try {
        val error = respuesta.body<ErrorDto>()
        val detalle = error.validationErrors?.values?.joinToString(" · ")
        when {
            respuesta.status == HttpStatusCode.TooManyRequests ->
                error.message ?: "Demasiados intentos. Espere un minuto e intente de nuevo."
            !detalle.isNullOrBlank() -> detalle
            !error.message.isNullOrBlank() -> error.message
            else -> "Error ${respuesta.status.value}"
        }
    } catch (e: Exception) {
        "Error ${respuesta.status.value}"
    }
}
