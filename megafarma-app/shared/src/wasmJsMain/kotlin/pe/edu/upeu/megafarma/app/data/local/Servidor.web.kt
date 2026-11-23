package pe.edu.upeu.megafarma.app.data.local

/* Nombre del host donde se abrio la pagina (localhost en la laptop, github.io en internet). */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun hostActual(): String = js("window.location.hostname")

/*
 * En la laptop (localhost) la pagina usa el backend local.
 * Publicada en GitHub Pages usa el backend desplegado en Render.
 * El usuario puede cambiarlo igual desde la pantalla de inicio de sesion.
 */
actual fun servidorPorDefecto(): String {
    val host = hostActual()
    return if (host == "localhost" || host == "127.0.0.1") {
        "http://localhost:8080"
    } else {
        "https://megafarma-backend.onrender.com"
    }
}
