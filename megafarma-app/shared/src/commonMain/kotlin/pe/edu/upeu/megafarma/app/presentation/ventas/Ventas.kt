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
import pe.edu.upeu.megafarma.app.core.esFechaValida
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

/* Tamano de pagina de la busqueda de ventas. */
private const val TAMANIO_PAGINA = 20

data class VentasUiState(
    // Filtros (HU-11). Vacios = mes actual.
    val desde: String = "",
    val hasta: String = "",
    val filtroAplicado: String = "Mes actual",
    // Resultados y paginacion (CA-11-05)
    val cargando: Boolean = true,
    val ventas: List<Venta> = emptyList(),
    val pagina: Int = 0,
    val totalPaginas: Int = 0,
    val totalElementos: Long = 0,
    val error: String? = null,
    // Detalle (HU-10) y anulacion (HU-12)
    val seleccionada: Venta? = null,
    val cargandoDetalle: Boolean = false,
    val confirmandoAnulacion: Boolean = false,
    val anulando: Boolean = false,
    val errorAnular: String? = null,
    val aviso: String? = null
)

/*
 * Busqueda de ventas por fecha con paginas (HU-11), detalle siempre actualizado
 * (HU-10) y anulacion con motivo y confirmacion explicita (HU-12, solo ADMIN).
 */
class VentasViewModel(private val repositorio: VentaRepository) : ViewModel() {

    private val _estado = MutableStateFlow(VentasUiState())
    val estado: StateFlow<VentasUiState> = _estado.asStateFlow()

    fun cargar() = consultar(0)

    fun cambiarDesde(valor: String) = _estado.update { it.copy(desde = valor) }
    fun cambiarHasta(valor: String) = _estado.update { it.copy(hasta = valor) }

    /* CA-11-01 y CA-11-03: valida las fechas antes de enviar la busqueda. */
    fun buscar() {
        val e = _estado.value
        val desde = e.desde.trim()
        val hasta = e.hasta.trim()
        val error = when {
            desde.isNotEmpty() && !esFechaValida(desde) -> "La fecha 'desde' debe tener el formato AAAA-MM-DD"
            hasta.isNotEmpty() && !esFechaValida(hasta) -> "La fecha 'hasta' debe tener el formato AAAA-MM-DD"
            desde.isNotEmpty() && hasta.isNotEmpty() && desde > hasta -> "La fecha 'desde' no puede ser posterior a 'hasta'"
            else -> null
        }
        if (error != null) {
            _estado.update { it.copy(error = error, ventas = emptyList(), totalElementos = 0, totalPaginas = 0) }
            return
        }
        consultar(0)
    }

    /* CA-11-06: sin filtros se vuelve al mes actual. */
    fun limpiar() {
        _estado.update { it.copy(desde = "", hasta = "") }
        consultar(0)
    }

    fun paginaSiguiente() {
        val e = _estado.value
        if (e.pagina + 1 < e.totalPaginas) consultar(e.pagina + 1)
    }

    fun paginaAnterior() {
        val e = _estado.value
        if (e.pagina > 0) consultar(e.pagina - 1)
    }

    private fun consultar(pagina: Int) {
        val e = _estado.value
        val desde = e.desde.trim().ifEmpty { null }
        val hasta = e.hasta.trim().ifEmpty { null }
        // Una sola fecha = ese dia (CA-11-01).
        val desdeFinal = desde ?: hasta
        val hastaFinal = hasta ?: desde
        val etiqueta = when {
            desdeFinal == null -> "Mes actual"
            desdeFinal == hastaFinal -> "Día $desdeFinal"
            else -> "Del $desdeFinal al $hastaFinal"
        }
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val r = repositorio.listar(desdeFinal, hastaFinal, pagina, TAMANIO_PAGINA)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(
                        cargando = false,
                        ventas = r.datos.ventas,
                        pagina = r.datos.pagina,
                        totalPaginas = r.datos.totalPaginas,
                        totalElementos = r.datos.totalElementos,
                        filtroAplicado = etiqueta
                    )
                }
                is Resultado.Error -> _estado.update { it.copy(cargando = false, error = r.mensaje) }
            }
        }
    }

    /* CA-10 / R06: al abrir el detalle se consulta de nuevo al servidor (puede haber cambiado). */
    fun seleccionar(venta: Venta?) {
        _estado.update { it.copy(seleccionada = venta, errorAnular = null, confirmandoAnulacion = false) }
        if (venta == null) return
        _estado.update { it.copy(cargandoDetalle = true) }
        viewModelScope.launch {
            when (val r = repositorio.buscar(venta.id)) {
                is Resultado.Exito -> _estado.update { it.copy(cargandoDetalle = false, seleccionada = r.datos) }
                is Resultado.Error -> _estado.update { it.copy(cargandoDetalle = false, errorAnular = r.mensaje) }
            }
        }
    }

    fun cerrarAviso() = _estado.update { it.copy(aviso = null) }

    /* CA-12-02: primero se pide el motivo; luego una confirmacion explicita. */
    fun pedirConfirmacion(motivo: String) {
        if (motivo.isBlank()) {
            _estado.update { it.copy(errorAnular = "Indique el motivo de la anulación") }
            return
        }
        _estado.update { it.copy(confirmandoAnulacion = true, errorAnular = null) }
    }

    fun cancelarConfirmacion() = _estado.update { it.copy(confirmandoAnulacion = false) }

    fun anular(venta: Venta, motivo: String) {
        _estado.update { it.copy(anulando = true, errorAnular = null) }
        viewModelScope.launch {
            when (val r = repositorio.anular(venta.id, motivo.trim())) {
                is Resultado.Exito -> {
                    _estado.update {
                        it.copy(
                            anulando = false, confirmandoAnulacion = false, seleccionada = null,
                            aviso = "Venta ${r.datos.numero} anulada: el stock volvió a sus lotes"
                        )
                    }
                    consultar(_estado.value.pagina)
                }
                is Resultado.Error -> _estado.update {
                    it.copy(anulando = false, confirmandoAnulacion = false, errorAnular = r.mensaje)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VentasScreen(sesion: Sesion, viewModel: VentasViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargar() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // ---------- Filtros por fecha (HU-11) ----------
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Campo(estado.desde, viewModel::cambiarDesde, "Desde (AAAA-MM-DD)", Modifier.widthIn(min = 170.dp, max = 220.dp))
            Campo(estado.hasta, viewModel::cambiarHasta, "Hasta (AAAA-MM-DD)", Modifier.widthIn(min = 170.dp, max = 220.dp))
            Button(onClick = viewModel::buscar) { Text("Buscar") }
            OutlinedButton(onClick = viewModel::limpiar) { Text("Limpiar") }
        }

        Text(
            "${estado.filtroAplicado} · ${estado.totalElementos} venta(s)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        estado.aviso?.let { Aviso(it, esError = false, onCerrar = viewModel::cerrarAviso) }

        Box(Modifier.weight(1f)) {
            when {
                estado.cargando && estado.ventas.isEmpty() -> Cargando()
                estado.error != null -> MensajeError(estado.error!!) { viewModel.cargar() }
                estado.ventas.isEmpty() -> Vacio("No hay ventas en ese período")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(estado.ventas, key = { it.id }) { venta ->
                        TarjetaVenta(venta) { viewModel.seleccionar(venta) }
                    }
                }
            }
        }

        // ---------- Paginas (CA-11-05) ----------
        if (estado.totalPaginas > 1) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = viewModel::paginaAnterior, enabled = estado.pagina > 0) { Text("< Anterior") }
                Text(
                    "Página ${estado.pagina + 1} de ${estado.totalPaginas}",
                    Modifier.weight(1f).padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = viewModel::paginaSiguiente,
                    enabled = estado.pagina + 1 < estado.totalPaginas
                ) { Text("Siguiente >") }
            }
        }
    }

    estado.seleccionada?.let { venta ->
        DetalleVentaDialog(
            venta = venta,
            cargando = estado.cargandoDetalle,
            puedeAnular = sesion.puede(Permiso.VENTAS_ANULAR) && !venta.anulada,
            anulando = estado.anulando,
            confirmando = estado.confirmandoAnulacion,
            error = estado.errorAnular,
            onPedirConfirmacion = viewModel::pedirConfirmacion,
            onAnular = { motivo -> viewModel.anular(venta, motivo) },
            onCancelarConfirmacion = viewModel::cancelarConfirmacion,
            onCerrar = { viewModel.seleccionar(null) }
        )
    }
}

@Composable
private fun TarjetaVenta(venta: Venta, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }) {
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

@Composable
private fun DetalleVentaDialog(
    venta: Venta,
    cargando: Boolean,
    puedeAnular: Boolean,
    anulando: Boolean,
    confirmando: Boolean,
    error: String?,
    onPedirConfirmacion: (String) -> Unit,
    onAnular: (String) -> Unit,
    onCancelarConfirmacion: () -> Unit,
    onCerrar: () -> Unit
) {
    var motivo by remember(venta.id) { mutableStateOf("") }

    // Segunda ventana: confirmacion explicita antes de anular (CA-12-02).
    if (confirmando) {
        val unidades = venta.detalles.sumOf { it.cantidad }
        AlertDialog(
            onDismissRequest = { if (!anulando) onCancelarConfirmacion() },
            title = { Text("¿Anular la venta ${venta.numero}?") },
            text = {
                Text("Se devolverán $unidades unidad(es) a sus lotes y la venta quedará como ANULADA. Esta acción no se puede deshacer.")
            },
            confirmButton = {
                Button(
                    onClick = { onAnular(motivo) },
                    enabled = !anulando,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(if (anulando) "Anulando…" else "Sí, anular") }
            },
            dismissButton = { TextButton(onClick = onCancelarConfirmacion, enabled = !anulando) { Text("No") } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = { if (!anulando) onCerrar() },
        title = { Text("Venta ${venta.numero}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("${venta.fecha.fechaHora()} · vendedor: ${venta.vendedor}")
                Text("Medio de pago: ${venta.medioPago}")
                venta.clienteNombre?.let { Text("Cliente: $it") }
                HorizontalDivider()
                venta.detalles.forEach { d ->
                    Text("${d.cantidad} x ${d.productoNombre} · ${d.precioUnitario.soles()} c/u · ${d.subtotal.soles()}")
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
                    onClick = { onPedirConfirmacion(motivo) },
                    enabled = !anulando && !cargando,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Anular venta") }
            } else {
                Button(onClick = onCerrar) { Text("Cerrar") }
            }
        },
        dismissButton = if (puedeAnular) ({ TextButton(onClick = onCerrar, enabled = !anulando) { Text("Cerrar") } }) else null
    )
}
