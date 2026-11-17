package pe.edu.upeu.megafarma.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.koin.compose.koinInject
import pe.edu.upeu.megafarma.app.domain.repository.AuthRepository
import pe.edu.upeu.megafarma.app.presentation.login.LoginScreen
import pe.edu.upeu.megafarma.app.presentation.navegacion.PantallaPrincipal
import pe.edu.upeu.megafarma.app.presentation.tema.MegafarmaTema

/*
 * Raiz de la interfaz, compartida por Android, iOS y Web.
 * Si no hay sesion muestra el login; si la hay, la pantalla principal.
 * Cuando el token vence, la sesion se cierra sola y vuelve al login.
 */
@Composable
fun App() {
    val sistemaOscuro = isSystemInDarkTheme()
    var oscuro by remember { mutableStateOf(sistemaOscuro) }

    MegafarmaTema(oscuro = oscuro) {
        val auth = koinInject<AuthRepository>()
        val sesion by auth.sesion.collectAsState()
        val actual = sesion

        if (actual == null) {
            LoginScreen()
        } else {
            PantallaPrincipal(
                sesion = actual,
                oscuro = oscuro,
                onCambiarTema = { oscuro = !oscuro },
                onCerrarSesion = { auth.cerrarSesion() }
            )
        }
    }
}
