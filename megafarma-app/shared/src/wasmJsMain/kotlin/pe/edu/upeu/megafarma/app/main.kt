package pe.edu.upeu.megafarma.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import pe.edu.upeu.megafarma.app.di.initKoin

/* Punto de entrada de la pagina web: arranca Koin y dibuja la misma App de Compose en el navegador. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    quitarMensajeDeCarga()
    initKoin()
    ComposeViewport {
        App()
    }
}

private fun quitarMensajeDeCarga() {
    js("var e = document.getElementById('cargando'); if (e) { e.remove(); }")
}
