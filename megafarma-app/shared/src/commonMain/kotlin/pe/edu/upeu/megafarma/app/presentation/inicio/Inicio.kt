package pe.edu.upeu.megafarma.app.presentation.inicio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import pe.edu.upeu.megafarma.app.domain.model.ResumenAlertas
import pe.edu.upeu.megafarma.app.domain.model.Sesion
import pe.edu.upeu.megafarma.app.domain.repository.InventarioRepository
import pe.edu.upeu.megafarma.app.presentation.componentes.Cargando
import pe.edu.upeu.megafarma.app.presentation.componentes.MensajeError
import pe.edu.upeu.megafarma.app.presentation.componentes.TarjetaDato
import pe.edu.upeu.megafarma.app.presentation.componentes.Titulo
import pe.edu.upeu.megafarma.app.presentation.navegacion.Destino
import pe.edu.upeu.megafarma.app.presentation.tema.ColoresEstado

data class InicioUiState(
    val cargando: Boolean = true,
    val resumen: ResumenAlertas? = null,
    val error: String? = null
)

/* Panel de inicio: contadores de alertas (HU-41). */
class InicioViewModel(private val inventario: InventarioRepository) : ViewModel() {

    private val _estado = MutableStateFlow(InicioUiState())
    val estado: StateFlow<InicioUiState> = _estado.asStateFlow()

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val r = inventario.resumenAlertas()) {
                is Resultado.Exito -> _estado.update { it.copy(cargando = false, resumen = r.datos) }
                is Resultado.Error -> _estado.update { it.copy(cargando = false, error = r.mensaje) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InicioScreen(sesion: Sesion, onIrA: (Destino) -> Unit, viewModel: InicioViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargar() }

    val destinos = Destino.entries.filter { it != Destino.INICIO && (it.permiso == null || sesion.puede(it.permiso)) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text("Hola, ${sesion.nombreCompleto}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("Rol: ${sesion.rol}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Titulo("Alertas del inventario")
        when {
            estado.cargando -> Cargando()
            estado.error != null -> MensajeError(estado.error!!) { viewModel.cargar() }
            estado.resumen != null -> {
                val r = estado.resumen!!
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TarjetaDato(
                        "Productos con stock bajo", r.productosBajoMinimo.toString(),
                        if (r.productosBajoMinimo > 0) ColoresEstado.alerta else ColoresEstado.ok,
                        Modifier.widthIn(min = 200.dp), "En su stock mínimo o debajo",
                        onClick = { onIrA(Destino.ALERTAS) }
                    )
                    TarjetaDato(
                        "Lotes por vencer", r.lotesPorVencer.toString(),
                        if (r.lotesPorVencer > 0) ColoresEstado.alerta else ColoresEstado.ok,
                        Modifier.widthIn(min = 200.dp), "En los próximos ${r.diasConsiderados} días",
                        onClick = { onIrA(Destino.ALERTAS) }
                    )
                    TarjetaDato(
                        "Lotes vencidos", r.lotesVencidos.toString(),
                        if (r.lotesVencidos > 0) ColoresEstado.peligro else ColoresEstado.ok,
                        Modifier.widthIn(min = 200.dp), "Con stock, pendientes de baja",
                        onClick = { onIrA(Destino.ALERTAS) }
                    )
                }
            }
        }

        Titulo("Accesos rápidos")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            destinos.forEach { d ->
                Button(onClick = { onIrA(d) }) { Text(d.titulo) }
            }
        }
    }
}
