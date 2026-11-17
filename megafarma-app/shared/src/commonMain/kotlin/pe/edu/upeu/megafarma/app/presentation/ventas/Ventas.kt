package pe.edu.upeu.megafarma.app.presentation.ventas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import pe.edu.upeu.megafarma.app.core.fechaHora
import pe.edu.upeu.megafarma.app.core.soles
import pe.edu.upeu.megafarma.app.domain.model.Permiso
import pe.edu.upeu.megafarma.app.domain.model.Sesion
import pe.edu.upeu.megafarma.app.domain.model.Venta
import pe.edu.upeu.megafarma.app.domain.repository.VentaRepository
import pe.edu.upeu.megafarma.app.presentation.componentes.Aviso
import pe.edu.upeu.megafarma.app.presentation.componentes.Campo
import pe.edu.upeu.megafarma.app.presentation.componentes.Cargando
import pe.edu.upeu.megafarma.app.presentation.componentes.Etiqueta
import pe.edu.upeu.megafarma.app.presentation.componentes.MensajeError
import pe.edu.upeu.megafarma.app.presentation.componentes.Vacio
import pe.edu.upeu.megafarma.app.presentation.tema.ColoresEstado

data class VentasUiState(
    val cargando: Boolean = true,
    val ventas: List<Venta> = emptyList(),
    val error: String? = null,
    val seleccionada: Venta? = null,
    val anulando: Boolean = false,
    val errorAnular: String? = null,
    val aviso: String? = null
)

/* Ventas del mes (HU-11), detalle (HU-10) y anulacion (HU-12, solo ADMIN). */
class VentasViewModel(private val repositorio: VentaRepository) : ViewModel() {

    private val _estado = MutableStateFlow(VentasUiState())
    val estado: StateFlow<VentasUiState> = _estado.asStateFlow()

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val r = repositorio.listar(null, null)) {
                is Resultado.Exito -> _estado.update { it.copy(cargando = false, ventas = r.datos) }
                is Resultado.Error -> _estado.update { it.copy(cargando = false, error = r.mensaje) }
            }
        }
    }

    fun seleccionar(venta: Venta?) = _estado.update { it.copy(seleccionada = venta, errorAnular = null) }

    fun cerrarAviso() = _estado.update { it.copy(aviso = null) }

    fun anular(venta: Venta, motivo: String) {
        if (motivo.isBlank()) {
            _estado.update { it.copy(errorAnular = "Indique el motivo de la anulación") }
            return
        }
        _estado.update { it.copy(anulando = true, errorAnular = null) }
        viewModelScope.launch {
            when (val r = repositorio.anular(venta.id, motivo.trim())) {
                is Resultado.Exito -> {
                    _estado.update {
                        it.copy(anulando = false, seleccionada = null, aviso = "Venta ${r.datos.numero} anulada: el stock volvió a sus lotes")
                    }
                    cargar()
                }
                is Resultado.Error -> _estado.update { it.copy(anulando = false, errorAnular = r.mensaje) }
            }
        }
    }
}

@Composable
fun VentasScreen(sesion: Sesion, viewModel: VentasViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargar() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val registradas = estado.ventas.filter { !it.anulada }
        Text(
            "Ventas del mes: ${registradas.size} · Total ${registradas.sumOf { it.total }.soles()}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        estado.aviso?.let { Aviso(it, esError = false, onCerrar = viewModel::cerrarAviso) }

        when {
            estado.cargando && estado.ventas.isEmpty() -> Cargando()
            estado.error != null -> MensajeError(estado.error!!) { viewModel.cargar() }
            estado.ventas.isEmpty() -> Vacio("Todavía no hay ventas este mes")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(estado.ventas, key = { it.id }) { venta ->
                    Card(Modifier.fillMaxWidth().clickable { viewModel.seleccionar(venta) }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(venta.numero, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${venta.fecha.fechaHora()} · ${venta.vendedor} · ${venta.medioPago}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(venta.total.soles(), fontWeight = FontWeight.Bold)
                                if (venta.anulada) Etiqueta("Anulada", ColoresEstado.peligro)
                                else Etiqueta("Registrada", ColoresEstado.ok)
                            }
                        }
                    }
                }
            }
        }
    }

    estado.seleccionada?.let { venta ->
        DetalleVentaDialog(
            venta = venta,
            puedeAnular = sesion.puede(Permiso.VENTAS_ANULAR) && !venta.anulada,
            anulando = estado.anulando,
            error = estado.errorAnular,
            onAnular = { motivo -> viewModel.anular(venta, motivo) },
            onCerrar = { viewModel.seleccionar(null) }
        )
    }
}

@Composable
private fun DetalleVentaDialog(
    venta: Venta,
    puedeAnular: Boolean,
    anulando: Boolean,
    error: String?,
    onAnular: (String) -> Unit,
    onCerrar: () -> Unit
) {
    var motivo by remember(venta.id) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!anulando) onCerrar() },
        title = { Text("Venta ${venta.numero}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${venta.fecha.fechaHora()} · vendedor: ${venta.vendedor}")
                Text("Medio de pago: ${venta.medioPago}")
                venta.clienteNombre?.let { Text("Cliente: $it") }
                HorizontalDivider()
                venta.detalles.forEach { d ->
                    Text("${d.cantidad} x ${d.productoNombre} · ${d.subtotal.soles()}")
                    Text(
                        "   Lote ${d.numeroLote} (vence ${d.fechaVencimiento})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()
                Text("Total: ${venta.total.soles()}", fontWeight = FontWeight.Bold)
                if (venta.anulada) {
                    Etiqueta("Anulada", ColoresEstado.peligro)
                    venta.motivoAnulacion?.let { Text("Motivo: $it") }
                }
                if (puedeAnular) {
                    Spacer(Modifier.height(8.dp))
                    Campo(motivo, { motivo = it }, "Motivo de anulación")
                }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            if (puedeAnular) {
                Button(
                    onClick = { onAnular(motivo) },
                    enabled = !anulando,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(if (anulando) "Anulando…" else "Anular venta") }
            } else {
                Button(onClick = onCerrar) { Text("Cerrar") }
            }
        },
        dismissButton = if (puedeAnular) ({ TextButton(onClick = onCerrar, enabled = !anulando) { Text("Cerrar") } }) else null
    )
}
