package pe.edu.upeu.megafarma.app.presentation.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.megafarma.app.core.Resultado
import pe.edu.upeu.megafarma.app.domain.model.Categoria
import pe.edu.upeu.megafarma.app.domain.model.DatosProducto
import pe.edu.upeu.megafarma.app.domain.model.Producto
import pe.edu.upeu.megafarma.app.domain.repository.ProductoRepository

data class ProductosUiState(
    val texto: String = "",
    val incluirInactivos: Boolean = false,
    val cargando: Boolean = true,
    val productos: List<Producto> = emptyList(),
    val error: String? = null,
    val formularioAbierto: Boolean = false,
    val categorias: List<Categoria> = emptyList(),
    val guardando: Boolean = false,
    val errorFormulario: String? = null,
    val aviso: String? = null
)

/* Listado y busqueda de productos (HU-03) y registro de productos nuevos (HU-01). */
class ProductosViewModel(private val repositorio: ProductoRepository) : ViewModel() {

    private val _estado = MutableStateFlow(ProductosUiState())
    val estado: StateFlow<ProductosUiState> = _estado.asStateFlow()

    private var busqueda: Job? = null

    fun cargar() = buscarAhora()

    /* Busca 350 ms despues de la ultima tecla, para no llamar a la API en cada letra. */
    fun cambiarTexto(texto: String) {
        _estado.update { it.copy(texto = texto) }
        busqueda?.cancel()
        busqueda = viewModelScope.launch {
            delay(350)
            ejecutarBusqueda()
        }
    }

    fun alternarInactivos() {
        _estado.update { it.copy(incluirInactivos = !it.incluirInactivos) }
        buscarAhora()
    }

    private fun buscarAhora() {
        busqueda?.cancel()
        busqueda = viewModelScope.launch { ejecutarBusqueda() }
    }

    private suspend fun ejecutarBusqueda() {
        _estado.update { it.copy(cargando = true, error = null) }
        val actual = _estado.value
        when (val r = repositorio.buscar(actual.texto, actual.incluirInactivos)) {
            is Resultado.Exito -> _estado.update { it.copy(cargando = false, productos = r.datos) }
            is Resultado.Error -> _estado.update { it.copy(cargando = false, error = r.mensaje) }
        }
    }

    fun abrirFormulario() {
        _estado.update { it.copy(formularioAbierto = true, errorFormulario = null) }
        if (_estado.value.categorias.isEmpty()) {
            viewModelScope.launch {
                when (val r = repositorio.categorias()) {
                    is Resultado.Exito -> _estado.update { it.copy(categorias = r.datos) }
                    is Resultado.Error -> _estado.update { it.copy(errorFormulario = r.mensaje) }
                }
            }
        }
    }

    fun cerrarFormulario() = _estado.update { it.copy(formularioAbierto = false) }

    fun guardar(datos: DatosProducto) {
        _estado.update { it.copy(guardando = true, errorFormulario = null) }
        viewModelScope.launch {
            when (val r = repositorio.guardar(null, datos)) {
                is Resultado.Exito -> {
                    _estado.update {
                        it.copy(guardando = false, formularioAbierto = false, aviso = "Producto ${r.datos.codigo} registrado correctamente")
                    }
                    buscarAhora()
                }
                is Resultado.Error -> _estado.update { it.copy(guardando = false, errorFormulario = r.mensaje) }
            }
        }
    }

    fun cerrarAviso() = _estado.update { it.copy(aviso = null) }
}
