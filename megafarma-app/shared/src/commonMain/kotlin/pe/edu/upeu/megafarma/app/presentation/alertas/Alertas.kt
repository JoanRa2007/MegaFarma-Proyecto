package pe.edu.upeu.megafarma.app.presentation.alertas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import pe.edu.upeu.megafarma.app.domain.model.Lote
import pe.edu.upeu.megafarma.app.domain.model.StockProducto
import pe.edu.upeu.megafarma.app.domain.repository.InventarioRepository
import pe.edu.upeu.megafarma.app.presentation.componentes.Cargando
import pe.edu.upeu.megafarma.app.presentation.componentes.Etiqueta
import pe.edu.upeu.megafarma.app.presentation.componentes.MensajeError
import pe.edu.upeu.megafarma.app.presentation.componentes.Vacio
import pe.edu.upeu.megafarma.app.presentation.tema.ColoresEstado

data class AlertasUiState(
    val cargando: Boolean = true,
    val stockMinimo: List<StockProducto> = emptyList(),
    val vencimientos: List<Lote> = emptyList(),
    val dias: Int = 60,
    val verVencimientos: Boolean = false,
    val error: String? = null
)

/* Alertas de stock minimo (HU-08) y de vencimientos (HU-13). */
class AlertasViewModel(private val inventario: InventarioRepository) : ViewModel() {

    private val _estado = MutableStateFlow(AlertasUiState())
    val estado: StateFlow<AlertasUiState> = _estado.asStateFlow()

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val minimo = inventario.stockMinimo()
            val vencen = inventario.vencimientos(_estado.value.dias)
            _estado.update {
                it.copy(
                    cargando = false,
                    stockMinimo = (minimo as? Resultado.Exito)?.datos ?: emptyList(),
                    vencimientos = (vencen as? Resultado.Exito)?.datos ?: emptyList(),
                    error = (minimo as? Resultado.Error)?.mensaje ?: (vencen as? Resultado.Error)?.mensaje
                )
            }
        }
    }

    fun mostrar(vencimientos: Boolean) = _estado.update { it.copy(verVencimientos = vencimientos) }

    fun cambiarDias(dias: Int) {
        _estado.update { it.copy(dias = dias) }
        cargar()
    }
}

@Composable
fun AlertasScreen(viewModel: AlertasViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargar() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !estado.verVencimientos,
                onClick = { viewModel.mostrar(false) },
                label = { Text("Stock mínimo (${estado.stockMinimo.size})") }
            )
            FilterChip(
                selected = estado.verVencimientos,
                onClick = { viewModel.mostrar(true) },
                label = { Text("Vencimientos (${estado.vencimientos.size})") }
            )
        }
        if (estado.verVencimientos) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Vencen en:")
                listOf(30, 60, 90).forEach { d ->
                    FilterChip(selected = estado.dias == d, onClick = { viewModel.cambiarDias(d) }, label = { Text("$d días") })
                }
            }
        }

        when {
            estado.cargando -> Cargando()
            estado.error != null -> MensajeError(estado.error!!) { viewModel.cargar() }
            !estado.verVencimientos -> {
                if (estado.stockMinimo.isEmpty()) Vacio("Ningún producto está en su stock mínimo")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(estado.stockMinimo, key = { it.productoId }) { s ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(s.nombre, fontWeight = FontWeight.SemiBold)
                                    Text(s.codigo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Disponible: ${s.stockDisponible}", color = ColoresEstado.alerta, fontWeight = FontWeight.Bold)
                                    Text("Mínimo: ${s.stockMinimo}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                if (estado.vencimientos.isEmpty()) Vacio("No hay lotes por vencer en ${estado.dias} días")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(estado.vencimientos, key = { it.id }) { l ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(l.productoNombre, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Lote ${l.numeroLote} · vence ${l.fechaVencimiento} · ${l.cantidadActual} unidades",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (l.vencido) Etiqueta("Vencido", ColoresEstado.peligro)
                                else Etiqueta("En ${l.diasParaVencer} días", ColoresEstado.alerta)
                            }
                        }
                    }
                }
            }
        }
    }
}
