package pe.edu.upeu.megafarma.app

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import pe.edu.upeu.megafarma.app.data.local.AlmacenLocal

/* Android guarda la sesion en SharedPreferences (privadas de la app). */
class AlmacenAndroid(contexto: Context) : AlmacenLocal {
    private val prefs = contexto.getSharedPreferences("megafarma", Context.MODE_PRIVATE)
    override fun leer(clave: String): String? = prefs.getString(clave, null)
    override fun guardar(clave: String, valor: String) = prefs.edit().putString(clave, valor).apply()
    override fun borrar(clave: String) = prefs.edit().remove(clave).apply()
}

val moduloAndroid = module {
    single<AlmacenLocal> { AlmacenAndroid(androidContext()) }
}
