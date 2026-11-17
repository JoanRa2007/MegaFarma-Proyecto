package pe.edu.upeu.megafarma.app

import org.koin.dsl.module
import platform.Foundation.NSUserDefaults
import pe.edu.upeu.megafarma.app.data.local.AlmacenLocal

/* iOS guarda la sesion en NSUserDefaults. */
class AlmacenIos : AlmacenLocal {
    private val defaults = NSUserDefaults.standardUserDefaults
    override fun leer(clave: String): String? = defaults.stringForKey(clave)
    override fun guardar(clave: String, valor: String) = defaults.setObject(valor, forKey = clave)
    override fun borrar(clave: String) = defaults.removeObjectForKey(clave)
}

val moduloIos = module {
    single<AlmacenLocal> { AlmacenIos() }
}
