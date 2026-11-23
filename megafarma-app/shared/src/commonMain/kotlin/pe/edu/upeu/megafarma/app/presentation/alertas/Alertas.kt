package pe.edu.upeu.megafarma.app.presentation.alertas

import pe.edu.upeu.megafarma.app.presentation.componentes.Tarjeta
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import pe.edu.upeu.megafarma.app.domain.model.Lote
import pe.edu.upeu.megafarma.app.domain.model.StockProducto
import pe.edu.upeu.megafarma.app.domain.repository.InventarioRepository
import pe.edu.upeu.megafarma.app.presentation.componentes.Cargando
import pe.edu.upeu.megafarma.app.presentation.componentes.Etiqueta
import pe.edu.upeu.megafarma.app.presentation.componentes.Iconos
import pe.edu.upeu.megafarma.app.presentation.componentes.MensajeError
import pe.edu.upeu.megafarma.app.presentation.componentes.Vacio
import pe.edu.upeu.megafarma.app.presentation.tema.ColoresEstado

data class AlertasUiState(
    val cargando: Boolean = true,
    val stockMinimo: List<StockProducto> = emptyList(),
    val vencimientos: List<Lote> = emptyList(),
    val verVencimientos: Boolean = true,
    /* null = todos los niveles */
    val nivel: String? = null,
    /* Texto del buscador: filtra por producto, codigo o lote. */
    val texto: String = "",
    val error: String? = null
)

/* Alertas de stock minimo (HU-08) y de vencimiento clasificadas por nivel (HU-13, HU-14). */
class AlertasViewModel(private val inventario: InventarioRepository) : ViewModel() {

    private val _estado = MutableStateFlow(AlertasUiState())
    val estado: StateFlow<AlertasUiState> = _estado.asStateFlow()

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val minimo = inventario.stockMinimo()
            val vencen = inventario.vencimientos(90)
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

    fun filtrarNivel(nivel: String?) = _estado.update { it.copy(nivel = nivel) }

    fun buscar(texto: String) = _estado.update { it.copy(texto = texto) }
}

/* Color y texto de cada nivel de urgencia. */
fun colorNivel(nivel: String?): Color = when (nivel) {
    "CRITICO" -> ColoresEstado.peligro
    "MEDIO" -> ColoresEstado.alerta
    "BAJO" -> ColoresEstado.aviso
    else -> ColoresEstado.neutro
}

fun textoNivel(nivel: String?): String = when (nivel) {
    "CRITICO" -> "Crítico"
    "MEDIO" -> "Medio"
    "BAJO" -> "Bajo"
    else -> "Sin alerta"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlertasScreen(viewModel: AlertasViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargar() }

    val conteo = estado.vencimientos.groupingBy { it.nivelAlerta }.eachCount()
    val q = estado.texto.trim().lowercase()
    fun coincide(vararg campos: String?): Boolean = q.isEmpty() || campos.any { it?.lowercase()?.contains(q) == true }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = estado.texto,
            onValueChange = viewModel::buscar,
            placeholder = { Text(if (estado.verVencimientos) "Buscar por producto o lote" else "Buscar por producto o código") },
            leadingIcon = { Icon(Iconos.Buscar, null, Modifier.size(18.dp)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(
                selected = estado.verVencimientos,
                onClick = { viewModel.mostrar(true) },
                label = { Text("Vencimientos (${estado.vencimientos.size})") }
            )
            FilterChip(
                selected = !estado.verVencimientos,
                onClick = { viewModel.mostrar(false) },
                label = { Text("Stock mínimo (${estado.stockMinimo.size})") }
            )
        }
        if (estado.verVencimientos) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(selected = estado.nivel == null, onClick = { viewModel.filtrarNivel(null) }, label = { Text("Todos") })
                listOf("CRITICO", "MEDIO", "BAJO").forEach { n ->
                    FilterChip(
                        selected = estado.nivel == n,
                        onClick = { viewModel.filtrarNivel(n) },
                        label = { Text("${textoNivel(n)} (${conteo[n] ?: 0})", color = colorNivel(n)) }
                    )
                }
            }
            Text(
                "Crítico: vencido o vence en 30 días · Medio: en 31 a 60 días · Bajo: en 61 a 90 días",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when {
            estado.cargando -> Cargando()
            estado.error != null -> MensajeError(estado.error!!) { viewModel.cargar() }
            !estado.verVencimientos -> {
                val lista = estado.stockMinimo.filter { coincide(it.nombre, it.codigo) }
                if (estado.stockMinimo.isEmpty()) Vacio("Ningún producto está en su stock mínimo")
                else if (lista.isEmpty()) Vacio("Ningún producto coincide con \"${estado.texto}\"")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(lista, key = { it.productoId }) { s ->
                        Tarjeta(Modifier.fillMaxWidth()) {
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
                val lista = estado.vencimientos.filter {
                    (estado.nivel == null || it.nivelAlerta == estado.nivel) && coincide(it.productoNombre, it.numeroLote, it.productoCodigo)
                }
                if (lista.isEmpty()) Vacio(if (q.isEmpty()) "No hay lotes en ese nivel" else "Ningún lote coincide con \"${estado.texto}\"")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(lista, key = { it.id }) { l ->
                        Tarjeta(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(l.productoNombre, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Lote ${l.numeroLote} · vence ${l.fechaVencimiento} · ${l.cantidadActual} unidades",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Etiqueta(textoNivel(l.nivelAlerta), colorNivel(l.nivelAlerta))
                                    Text(
                                        if (l.vencido) "Vencido" else "En ${l.diasParaVencer} días",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
