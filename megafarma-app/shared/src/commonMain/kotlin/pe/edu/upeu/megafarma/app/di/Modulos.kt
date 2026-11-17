package pe.edu.upeu.megafarma.app.di

import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import pe.edu.upeu.megafarma.app.data.local.SesionManager
import pe.edu.upeu.megafarma.app.data.remote.MegafarmaApi
import pe.edu.upeu.megafarma.app.data.remote.crearHttpClient
import pe.edu.upeu.megafarma.app.data.repository.AuthRepositoryImpl
import pe.edu.upeu.megafarma.app.data.repository.InventarioRepositoryImpl
import pe.edu.upeu.megafarma.app.data.repository.ProductoRepositoryImpl
import pe.edu.upeu.megafarma.app.data.repository.ReporteRepositoryImpl
import pe.edu.upeu.megafarma.app.data.repository.VentaRepositoryImpl
import pe.edu.upeu.megafarma.app.domain.repository.AuthRepository
import pe.edu.upeu.megafarma.app.domain.repository.InventarioRepository
import pe.edu.upeu.megafarma.app.domain.repository.ProductoRepository
import pe.edu.upeu.megafarma.app.domain.repository.ReporteRepository
import pe.edu.upeu.megafarma.app.domain.repository.VentaRepository
import pe.edu.upeu.megafarma.app.domain.usecase.IniciarSesionUseCase
import pe.edu.upeu.megafarma.app.domain.usecase.RegistrarEntradaUseCase
import pe.edu.upeu.megafarma.app.domain.usecase.RegistrarVentaUseCase
import pe.edu.upeu.megafarma.app.presentation.alertas.AlertasViewModel
import pe.edu.upeu.megafarma.app.presentation.inicio.InicioViewModel
import pe.edu.upeu.megafarma.app.presentation.login.LoginViewModel
import pe.edu.upeu.megafarma.app.presentation.productos.ProductoDetalleViewModel
import pe.edu.upeu.megafarma.app.presentation.productos.ProductosViewModel
import pe.edu.upeu.megafarma.app.presentation.reportes.ReportesViewModel
import pe.edu.upeu.megafarma.app.presentation.ventas.NuevaVentaViewModel
import pe.edu.upeu.megafarma.app.presentation.ventas.VentasViewModel

/* Cada plataforma aporta su AlmacenLocal (SharedPreferences, localStorage, NSUserDefaults). */
expect val moduloPlataforma: Module

/* Capa de datos: JSON, sesion, cliente HTTP, API y repositorios. */
val moduloDatos = module {
    single {
        Json {
            ignoreUnknownKeys = true   // el backend envia mas campos de los que usa la app
            encodeDefaults = true      // envia tambien los valores por defecto (ej. requiereReceta=false)
            explicitNulls = false      // no envia los campos null
        }
    }
    single { SesionManager(get(), get()) }
    single { crearHttpClient(get(), get()) }
    single { MegafarmaApi(get(), get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<ProductoRepository> { ProductoRepositoryImpl(get()) }
    single<InventarioRepository> { InventarioRepositoryImpl(get()) }
    single<VentaRepository> { VentaRepositoryImpl(get()) }
    single<ReporteRepository> { ReporteRepositoryImpl(get()) }
}

/* Capa de dominio: casos de uso. */
val moduloDominio = module {
    factory { IniciarSesionUseCase(get()) }
    factory { RegistrarVentaUseCase(get()) }
    factory { RegistrarEntradaUseCase(get()) }
}

/* Capa de presentacion: un ViewModel por pantalla. */
val moduloPresentacion = module {
    viewModel { LoginViewModel(get(), get()) }
    viewModel { InicioViewModel(get()) }
    viewModel { ProductosViewModel(get()) }
    viewModel { ProductoDetalleViewModel(get(), get(), get()) }
    viewModel { NuevaVentaViewModel(get(), get()) }
    viewModel { VentasViewModel(get()) }
    viewModel { AlertasViewModel(get()) }
    viewModel { ReportesViewModel(get()) }
}

fun initKoin(config: KoinAppDeclaration = {}) = startKoin {
    config()
    modules(moduloPlataforma, moduloDatos, moduloDominio, moduloPresentacion)
}
