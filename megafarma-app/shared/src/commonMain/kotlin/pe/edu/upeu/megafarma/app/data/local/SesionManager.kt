package pe.edu.upeu.megafarma.app.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pe.edu.upeu.megafarma.app.domain.model.Sesion

/*
 * Guarda la sesion (token JWT y datos del usuario) y la direccion del servidor.
 * Se persiste en el dispositivo para no pedir login cada vez que se abre la app.
 */
class SesionManager(
    private val almacen: AlmacenLocal,
    private val json: Json
) {
    private val _sesion = MutableStateFlow(leerGuardada())
    val sesion: StateFlow<Sesion?> = _sesion.asStateFlow()

    var servidor: String = almacen.leer(CLAVE_SERVIDOR) ?: servidorPorDefecto()
        private set

    val token: String? get() = _sesion.value?.token

    fun cambiarServidor(url: String) {
        servidor = url
        almacen.guardar(CLAVE_SERVIDOR, url)
    }

    fun iniciar(sesion: Sesion) {
        almacen.guardar(CLAVE_SESION, json.encodeToString(SesionGuardada.serializer(), SesionGuardada.de(sesion)))
        _sesion.value = sesion
    }

    fun cerrar() {
        almacen.borrar(CLAVE_SESION)
        _sesion.value = null
    }

    private fun leerGuardada(): Sesion? = try {
        almacen.leer(CLAVE_SESION)?.let { json.decodeFromString(SesionGuardada.serializer(), it).aSesion() }
    } catch (e: Exception) {
        null
    }

    @Serializable
    private data class SesionGuardada(
        val token: String,
        val username: String,
        val nombreCompleto: String,
        val rol: String,
        val permisos: List<String>,
        val debeCambiarPassword: Boolean = false
    ) {
        fun aSesion() = Sesion(token, username, nombreCompleto, rol, permisos, debeCambiarPassword)

        companion object {
            fun de(s: Sesion) = SesionGuardada(s.token, s.username, s.nombreCompleto, s.rol, s.permisos, s.debeCambiarPassword)
        }
    }

    private companion object {
        const val CLAVE_SESION = "megafarma.sesion"
        const val CLAVE_SERVIDOR = "megafarma.servidor"
    }
}
