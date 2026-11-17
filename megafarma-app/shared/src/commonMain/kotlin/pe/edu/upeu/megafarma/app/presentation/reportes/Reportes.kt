package pe.edu.upeu.megafarma.app.presentation.reportes

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
import pe.edu.upeu.megafarma.app.core.soles
import pe.edu.upeu.megafarma.app.domain.model.GrupoReporte
import pe.edu.upeu.megafarma.app.domain.model.ProductoVendido
import pe.edu.upeu.megafarma.app.domain.model.ReporteInventario
import pe.edu.upeu.megafarma.app.domain.model.ReporteVentas
import pe.edu.upeu.megafarma.app.domain.repository.ReporteRepository
import pe.edu.upeu.megafarma.app.presentation.componentes.BarraHorizontal
import pe.edu.upeu.megafarma.app.presentation.componentes.Cargando
import pe.edu.upeu.megafarma.app.presentation.componentes.MensajeError
import pe.edu.upeu.megafarma.app.presentation.componentes.TarjetaDato
import pe.edu.upeu.megafarma.app.presentation.componentes.Titulo
import pe.edu.upeu.megafarma.app.presentation.componentes.Vacio
import pe.edu.upeu.megafarma.app.presentation.tema.ColoresEstado

data class ReportesUiState(
    val cargando: Boolean = true,
    val ventas: ReporteVentas? = null,
    val masVendidos: List<ProductoVendido> = emptyList(),
    val inventario: ReporteInventario? = null,
    val error: String? = null
)

/* Reportes de gestion (HU-17, HU-18, HU-19), solo para el administrador. */
class ReportesViewModel(private val repositorio: ReporteRepository) : ViewModel() {

    private val _estado = MutableStateFlow(ReportesUiState())
    val estado: StateFlow<ReportesUiState> = _estado.asStateFlow()

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val ventas = repositorio.ventas()
            val masVendidos = repositorio.masVendidos()
            val inventario = repositorio.inventario()
            val error = listOf(ventas, masVendidos, inventario).filterIsInstance<Resultado.Error>().firstOrNull()?.mensaje
            _estado.update {
                it.copy(
                    cargando = false,
                    ventas = (ventas as? Resultado.Exito)?.datos,
                    masVendidos = (masVendidos as? Resultado.Exito)?.datos ?: emptyList(),
                    inventario = (inventario as? Resultado.Exito)?.datos,
                    error = error
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

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val periodo = estado.ventas?.let { "Del ${it.desde} al ${it.hasta}" } ?: "Mes actual"
            Text(periodo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = viewModel::cargar) { Text("Actualizar") }
        }

        when {
            estado.cargando -> Cargando()
            estado.error != null -> MensajeError(estado.error!!) { viewModel.cargar() }
            else -> {
                estado.ventas?.let { v ->
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TarjetaDato("Total vendido", v.totalVendido.soles(), ColoresEstado.ok, Modifier.widthIn(min = 180.dp))
                        TarjetaDato("Ventas", v.cantidadVentas.toString(), MaterialTheme.colorScheme.primary, Modifier.widthIn(min = 180.dp))
                        TarjetaDato("Ticket promedio", v.ticketPromedio.soles(), MaterialTheme.colorScheme.secondary, Modifier.widthIn(min = 180.dp))
                        TarjetaDato("Anuladas", v.ventasAnuladas.toString(), ColoresEstado.peligro, Modifier.widthIn(min = 180.dp), v.totalAnulado.soles())
                    }
                    SeccionBarras("Ventas por medio de pago", v.porMedioPago)
                    SeccionBarras("Ventas por día", v.porDia)
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Titulo("Productos más vendidos")
                        if (estado.masVendidos.isEmpty()) Vacio("Sin ventas en el período")
                        estado.masVendidos.forEachIndexed { i, p ->
                            Row {
                                Text("${i + 1}. ${p.nombre}", Modifier.weight(1f))
                                Text("${p.unidades} u · ${p.importe.soles()}", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                estado.inventario?.let { inv ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Titulo("Inventario valorizado")
                            Text("Valor a precio de venta: ${inv.valorTotal.soles()}", fontWeight = FontWeight.Bold)
                            Text("Unidades disponibles: ${inv.unidadesTotales}")
                            Text("Productos con stock: ${inv.productosConStock}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeccionBarras(titulo: String, grupos: List<GrupoReporte>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Titulo(titulo)
            Spacer(Modifier.height(8.dp))
            if (grupos.isEmpty()) {
                Vacio("Sin datos en el período")
            } else {
                val maximo = grupos.maxOf { it.total }.takeIf { it > 0 } ?: 1.0
                grupos.forEach { g ->
                    BarraHorizontal(g.etiqueta, (g.total / maximo).toFloat(), g.total.soles())
                }
            }
        }
    }
}
