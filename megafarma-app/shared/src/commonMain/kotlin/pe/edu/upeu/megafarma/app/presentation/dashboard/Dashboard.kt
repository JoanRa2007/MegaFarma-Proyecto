package pe.edu.upeu.megafarma.app.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import pe.edu.upeu.megafarma.app.domain.model.Periodo
import pe.edu.upeu.megafarma.app.domain.model.ResumenVentas
import pe.edu.upeu.megafarma.app.domain.repository.ReporteRepository
import pe.edu.upeu.megafarma.app.presentation.componentes.Cargando
import pe.edu.upeu.megafarma.app.presentation.componentes.MensajeError
import pe.edu.upeu.megafarma.app.presentation.reportes.ResumenVentasVista

data class DashboardUiState(
    val periodo: Periodo = Periodo.HOY,
    val cargando: Boolean = true,
    val resumen: ResumenVentas? = null,
    val error: String? = null
)

/* Dashboard de ventas del punto de venta (solo ADMIN). */
class DashboardViewModel(private val reportes: ReporteRepository) : ViewModel() {

    private val _estado = MutableStateFlow(DashboardUiState())
    val estado: StateFlow<DashboardUiState> = _estado.asStateFlow()

    fun cargar(periodo: Periodo = _estado.value.periodo) {
        _estado.update { it.copy(periodo = periodo, cargando = true, error = null) }
        viewModelScope.launch {
            when (val r = reportes.dashboard(periodo)) {
                is Resultado.Exito -> _estado.update { it.copy(cargando = false, resumen = r.datos) }
                is Resultado.Error -> _estado.update { it.copy(cargando = false, error = r.mensaje) }
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargar() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(Periodo.HOY, Periodo.SEMANA, Periodo.MES, Periodo.ANIO).forEach { p ->
                FilterChip(selected = estado.periodo == p, onClick = { viewModel.cargar(p) }, label = { Text(p.etiqueta) })
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { viewModel.cargar() }) { Text("Actualizar") }
        }
        when {
            estado.cargando && estado.resumen == null -> Cargando()
            estado.error != null -> MensajeError(estado.error!!) { viewModel.cargar() }
            estado.resumen != null -> ResumenVentasVista(estado.resumen!!)
        }
    }
}
