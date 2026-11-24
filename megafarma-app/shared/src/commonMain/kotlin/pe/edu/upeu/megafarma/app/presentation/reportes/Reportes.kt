package pe.edu.upeu.megafarma.app.presentation.reportes

import pe.edu.upeu.megafarma.app.presentation.componentes.Tarjeta
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.megafarma.app.core.Resultado
import pe.edu.upeu.megafarma.app.core.esFechaValida
import pe.edu.upeu.megafarma.app.core.fechaCorta
import pe.edu.upeu.megafarma.app.core.soles
import pe.edu.upeu.megafarma.app.domain.model.Periodo
import pe.edu.upeu.megafarma.app.domain.model.ReporteInventario
import pe.edu.upeu.megafarma.app.domain.model.ResumenVentas
import pe.edu.upeu.megafarma.app.domain.repository.ReporteRepository
import pe.edu.upeu.megafarma.app.presentation.componentes.CampoFecha
import pe.edu.upeu.megafarma.app.presentation.componentes.Cargando
import pe.edu.upeu.megafarma.app.presentation.componentes.CabeceraResumen
import pe.edu.upeu.megafarma.app.presentation.componentes.PastillaPeriodo
import pe.edu.upeu.megafarma.app.presentation.componentes.MensajeError
import pe.edu.upeu.megafarma.app.presentation.componentes.Titulo

data class ReportesUiState(
    val tipo: Periodo = Periodo.DIA,
    val fecha: String = "",
    val cargando: Boolean = true,
    val resumen: ResumenVentas? = null,
    val inventario: ReporteInventario? = null,
    val error: String? = null
)

/* Reportes de ventas por dia, mes o año (HU-17 a HU-19) e inventario valorizado (HU-18). */
class ReportesViewModel(private val repositorio: ReporteRepository) : ViewModel() {

    private val _estado = MutableStateFlow(ReportesUiState())
    val estado: StateFlow<ReportesUiState> = _estado.asStateFlow()

    fun elegirTipo(tipo: Periodo) {
        _estado.update { it.copy(tipo = tipo) }
        cargar()
    }

    fun cambiarFecha(fecha: String) = _estado.update { it.copy(fecha = fecha) }

    fun cargar() {
        val e = _estado.value
        val fecha = e.fecha.trim()
        if (fecha.isNotEmpty() && !esFechaValida(fecha)) {
            _estado.update { it.copy(error = "La fecha debe tener el formato AAAA-MM-DD") }
            return
        }
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val resumen = repositorio.resumen(e.tipo, fecha.ifEmpty { null })
            val inventario = repositorio.inventario()
            _estado.update {
                it.copy(
                    cargando = false,
                    resumen = (resumen as? Resultado.Exito)?.datos,
                    inventario = (inventario as? Resultado.Exito)?.datos,
                    error = (resumen as? Resultado.Error)?.mensaje ?: (inventario as? Resultado.Error)?.mensaje
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportesScreen(viewModel: ReportesViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargar() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Cabecera con el periodo del reporte (sin repetir el titulo de la barra superior).
        val r = estado.resumen
        CabeceraResumen(
            etiqueta = "Análisis de ventas",
            titulo = r?.titulo ?: "Reporte por ${estado.tipo.etiqueta.lowercase()}",
            detalle = r?.let {
                if (it.desde == it.hasta) "${it.totalVendido.soles()} vendidos en ${it.cantidadVentas} venta(s)"
                else "Del ${it.desde.fechaCorta()} al ${it.hasta.fechaCorta()} · ${it.totalVendido.soles()} en ${it.cantidadVentas} venta(s)"
            },
            cargando = estado.cargando,
            opciones = {
                listOf(Periodo.DIA, Periodo.MES, Periodo.ANIO).forEach { t ->
                    PastillaPeriodo("Por ${t.etiqueta.lowercase()}", estado.tipo == t) { viewModel.elegirTipo(t) }
                }
            }
        )
        CampoFecha(
            estado.fecha,
            { viewModel.cambiarFecha(it); viewModel.cargar() },
            when (estado.tipo) {
                Periodo.MES -> "Mes del reporte"
                Periodo.ANIO -> "Año del reporte"
                else -> "Día del reporte"
            },
            Modifier.widthIn(max = 320.dp),
            ayuda = if (estado.tipo == Periodo.DIA) null else "Elija cualquier día de ese ${estado.tipo.etiqueta.lowercase()}",
            textoVacio = "Hoy"
        )

        when {
            estado.cargando && estado.resumen == null -> Cargando()
            estado.error != null -> MensajeError(estado.error!!) { viewModel.cargar() }
            else -> estado.resumen?.let { ResumenVentasVista(it, estado.inventario) }
        }
    }
}
