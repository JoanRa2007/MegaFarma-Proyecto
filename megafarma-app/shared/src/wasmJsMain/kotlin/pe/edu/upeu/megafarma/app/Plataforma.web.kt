package pe.edu.upeu.megafarma.app

import org.koin.dsl.module
import pe.edu.upeu.megafarma.app.data.local.AlmacenLocal

/* La web guarda la sesion en el localStorage del navegador. */
class AlmacenWeb : AlmacenLocal {
    override fun leer(clave: String): String? = leerLocalStorage(clave)
    override fun guardar(clave: String, valor: String) = guardarLocalStorage(clave, valor)
    override fun borrar(clave: String) = borrarLocalStorage(clave)
}

private fun leerLocalStorage(clave: String): String? = js("window.localStorage.getItem(clave)")
private fun guardarLocalStorage(clave: String, valor: String) { js("window.localStorage.setItem(clave, valor)") }
private fun borrarLocalStorage(clave: String) { js("window.localStorage.removeItem(clave)") }

val moduloWeb = module {
    single<AlmacenLocal> { AlmacenWeb() }
}
