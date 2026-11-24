package pe.edu.upeu.megafarma.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pe.edu.upeu.megafarma.app.data.local.AlmacenLocal
import pe.edu.upeu.megafarma.app.domain.repository.AuthRepository
import pe.edu.upeu.megafarma.app.presentation.login.LoginScreen
import pe.edu.upeu.megafarma.app.presentation.navegacion.PantallaPrincipal
import pe.edu.upeu.megafarma.app.presentation.perfil.CambioPasswordObligatorioScreen
import pe.edu.upeu.megafarma.app.presentation.tema.MegafarmaTema

/*
 * Raiz de la interfaz, compartida por Android, iOS y Web.
 * Si no hay sesion muestra el login; si la hay, la pantalla principal.
 * Cuando el token vence, la sesion se cierra sola y vuelve al login.
 */
@Composable
fun App() {
    val almacen = koinInject<AlmacenLocal>()
    // Modo claro por defecto; la eleccion de cada dispositivo queda guardada.
    var oscuro by remember { mutableStateOf(almacen.leer(CLAVE_TEMA) == "oscuro") }

    MegafarmaTema(oscuro = oscuro) {
        val auth = koinInject<AuthRepository>()
        val scope = rememberCoroutineScope()
        val sesion by auth.sesion.collectAsState()
        val actual = sesion

        if (actual == null) {
            LoginScreen()
        } else {
            // Cada sesion tiene sus propios ViewModels: al cerrar sesion se descartan,
            // asi el siguiente usuario no ve lo que dejo el anterior (caja, filtros, formularios).
            val propietario = remember(actual.username, actual.token) { ViewModelsDeSesion() }
            DisposableEffect(propietario) { onDispose { propietario.viewModelStore.clear() } }
            val cerrarSesion: () -> Unit = { scope.launch { auth.cerrarSesion() } }

            CompositionLocalProvider(LocalViewModelStoreOwner provides propietario) {
                if (actual.debeCambiarPassword) {
                    CambioPasswordObligatorioScreen(actual, onCerrarSesion = cerrarSesion)
                } else {
                    PantallaPrincipal(
                        sesion = actual,
                        oscuro = oscuro,
                        onCambiarTema = {
                            oscuro = !oscuro
                            almacen.guardar(CLAVE_TEMA, if (oscuro) "oscuro" else "claro")
                        },
                        onCerrarSesion = cerrarSesion
                    )
                }
            }
        }
    }
}

/* Contenedor de los ViewModels de una sesion; se limpia al cerrarla. */
private class ViewModelsDeSesion : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}

private const val CLAVE_TEMA = "megafarma.tema"
