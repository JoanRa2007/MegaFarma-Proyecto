package pe.edu.upeu.megafarma.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import pe.edu.upeu.megafarma.app.di.initKoin

/* Se ejecuta una sola vez al abrir la app: arranca Koin con el contexto de Android. */
class MegafarmaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MegafarmaApplication)
        }
    }
}
