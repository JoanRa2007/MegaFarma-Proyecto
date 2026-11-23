package pe.edu.upeu.megafarma.app.presentation.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import pe.edu.upeu.megafarma.app.presentation.tema.ColoresMarca
import androidx.compose.ui.graphics.Color
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
import pe.edu.upeu.megafarma.app.presentation.componentes.EncabezadoSeccion
import pe.edu.upeu.megafarma.app.presentation.componentes.Iconos
import pe.edu.upeu.megafarma.app.presentation.componentes.Insignia
import pe.edu.upeu.megafarma.app.presentation.componentes.GrillaTarjetas
import pe.edu.upeu.megafarma.app.presentation.componentes.Kpi
import pe.edu.upeu.megafarma.app.presentation.componentes.MensajeError
import pe.edu.upeu.megafarma.app.presentation.componentes.Panel
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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {

        // Bienvenida
        Box(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
                .background(Brush.horizontalGradient(ColoresMarca.degradado))
                .padding(28.dp)
        ) {
            Column {
                Text("Hola, ${sesion.nombreCompleto.substringBefore(" ")}", style = MaterialTheme.typography.headlineMedium,
                    color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Has ingresado como ${sesion.rol}. Desde aquí tienes acceso a ${destinos.size} módulos del sistema.",
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        EncabezadoSeccion("Estado del inventario", "Alertas que requieren atención")
        when {
            estado.cargando -> Cargando()
            estado.error != null -> MensajeError(estado.error!!) { viewModel.cargar() }
            estado.resumen != null -> {
                val r = estado.resumen!!
                GrillaTarjetas(anchoMinimo = 220.dp) {
                    val m = Modifier.weight(1f)
                    Kpi("Stock bajo", r.productosBajoMinimo.toString(), Iconos.Inventario,
                        if (r.productosBajoMinimo > 0) ColoresEstado.alerta else ColoresEstado.ok, m,
                        "Productos en su mínimo o debajo", onClick = { onIrA(Destino.ALERTAS) })
                    Kpi("Vencimiento crítico", r.lotesCriticos.toString(), Iconos.Advertencia,
                        if (r.lotesCriticos > 0) ColoresEstado.peligro else ColoresEstado.ok, m,
                        "Vencidos o en 30 días · ${r.lotesVencidos} vencidos", onClick = { onIrA(Destino.ALERTAS) })
                    Kpi("Vencimiento medio", r.lotesMedios.toString(), Iconos.Alerta,
                        if (r.lotesMedios > 0) ColoresEstado.alerta else ColoresEstado.ok, m,
                        "Vencen en 31 a 60 días", onClick = { onIrA(Destino.ALERTAS) })
                    Kpi("Vencimiento bajo", r.lotesBajos.toString(), Iconos.Ok,
                        if (r.lotesBajos > 0) ColoresEstado.aviso else ColoresEstado.ok, m,
                        "Vencen en 61 a 90 días", onClick = { onIrA(Destino.ALERTAS) })
                }
            }
        }

        EncabezadoSeccion("Accesos rápidos", "Módulos disponibles para tu rol")
        GrillaTarjetas(anchoMinimo = 150.dp) {
            destinos.forEach { d ->
                Panel(Modifier.weight(1f).fillMaxRowHeight(), onClick = { onIrA(d) }) {
                    Insignia(d.icono(), MaterialTheme.colorScheme.primary, 40)
                    Spacer(Modifier.height(12.dp))
                    Text(d.titulo, fontWeight = FontWeight.SemiBold)
                    Text(d.descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
