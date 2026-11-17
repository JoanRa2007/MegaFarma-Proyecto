package pe.edu.upeu.megafarma.app.data.local

/*
 * Guardado clave-valor en el dispositivo. Cada plataforma tiene su version:
 * Android = SharedPreferences, Web = localStorage, iOS = NSUserDefaults.
 */
interface AlmacenLocal {
    fun leer(clave: String): String?
    fun guardar(clave: String, valor: String)
    fun borrar(clave: String)
}

/* Direccion del backend por defecto en cada plataforma (expect/actual). */
expect fun servidorPorDefecto(): String
