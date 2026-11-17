package pe.edu.upeu.megafarma.app.presentation.ventas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.megafarma.app.core.Resultado
import pe.edu.upeu.megafarma.app.core.nuevaClave
import pe.edu.upeu.megafarma.app.core.soles
import pe.edu.upeu.megafarma.app.domain.model.LineaVenta
import pe.edu.upeu.megafarma.app.domain.model.MedioPago
import pe.edu.upeu.megafarma.app.domain.model.Producto
import pe.edu.upeu.megafarma.app.domain.model.Venta
import pe.edu.upeu.megafarma.app.domain.repository.ProductoRepository
import pe.edu.upeu.megafarma.app.domain.usecase.RegistrarVentaUseCase
import pe.edu.upeu.megafarma.app.presentation.componentes.Titulo

data class ItemCarrito(val producto: Producto, val cantidad: Int) {
    val subtotal: Double get() = producto.precioVenta * cantidad
}

data class NuevaVentaUiState(
    val texto: String = "",
    val buscando: Boolean = false,
    val resultados: List<Producto> = emptyList(),
    val carrito: List<ItemCarrito> = emptyList(),
    val medioPago: MedioPago = MedioPago.EFECTIVO,
    val registrando: Boolean = false,
    val error: String? = null,
    val ventaRegistrada: Venta? = null
) {
    val total: Double get() = carrito.sumOf { it.subtotal }
}

/* Registro de una venta (HU-09) con medio de pago (HU-34). El backend aplica FEFO (HU-29). */
class NuevaVentaViewModel(
    private val productos: ProductoRepository,
    private val registrarVenta: RegistrarVentaUseCase
) : ViewModel() {

    private val _estado = MutableStateFlow(NuevaVentaUiState())
    val estado: StateFlow<NuevaVentaUiState> = _estado.asStateFlow()

    private var busqueda: Job? = null

    /* Se conserva hasta que la venta se registre: un reintento no la duplica. */
    private var clave = nuevaClave()

    fun cargar() {
        busqueda?.cancel()
        busqueda = viewModelScope.launch { buscar() }
    }

    fun cambiarTexto(texto: String) {
        _estado.update { it.copy(texto = texto) }
        busqueda?.cancel()
        busqueda = viewModelScope.launch {
            delay(300)
            buscar()
        }
    }

    private suspend fun buscar() {
        _estado.update { it.copy(buscando = true) }
        when (val r = productos.buscar(_estado.value.texto)) {
            is Resultado.Exito -> _estado.update { it.copy(buscando = false, resultados = r.datos) }
            is Resultado.Error -> _estado.update { it.copy(buscando = false, error = r.mensaje) }
        }
    }

    fun agregar(producto: Producto) {
        val actual = _estado.value.carrito.firstOrNull { it.producto.id == producto.id }?.cantidad ?: 0
        if (actual + 1 > producto.stockDisponible) {
            _estado.update { it.copy(error = "Solo hay ${producto.stockDisponible} unidades disponibles de ${producto.nombre}") }
            return
        }
        cambiarCantidad(producto, +1)
    }

    fun cambiarCantidad(producto: Producto, delta: Int) {
        _estado.update { estado ->
            val existe = estado.carrito.any { it.producto.id == producto.id }
            val carrito = if (existe) {
                estado.carrito.map {
                    if (it.producto.id == producto.id) it.copy(cantidad = (it.cantidad + delta).coerceAtMost(producto.stockDisponible)) else it
                }.filter { it.cantidad > 0 }
            } else {
                estado.carrito + ItemCarrito(producto, delta.coerceAtLeast(1))
            }
            estado.copy(carrito = carrito, error = null)
        }
    }

    fun quitar(producto: Producto) = _estado.update { e -> e.copy(carrito = e.carrito.filter { it.producto.id != producto.id }) }

    fun elegirMedio(medio: MedioPago) = _estado.update { it.copy(medioPago = medio) }

    fun cerrarError() = _estado.update { it.copy(error = null) }

    fun cerrarVenta() = _estado.update { it.copy(ventaRegistrada = null) }

    fun registrar() {
        val actual = _estado.value
        if (actual.registrando) return
        _estado.update { it.copy(registrando = true, error = null) }
        viewModelScope.launch {
            val lineas = actual.carrito.map { LineaVenta(it.producto.id, it.cantidad) }
            when (val r = registrarVenta(lineas, actual.medioPago, clave)) {
                is Resultado.Exito -> {
                    clave = nuevaClave()
                    _estado.update { it.copy(registrando = false, carrito = emptyList(), ventaRegistrada = r.datos) }
                    buscar() // el stock cambio
                }
                is Resultado.Error -> _estado.update { it.copy(registrando = false, error = r.mensaje) }
            }
        }
    }
}

@Composable
fun NuevaVentaScreen(viewModel: NuevaVentaViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargar() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 840.dp) {
            // Pantalla ancha: buscador a la izquierda y carrito a la derecha.
            Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LazyColumn(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    seccionBuscador(estado, viewModel)
                }
                Card(Modifier.weight(1f).fillMaxHeight()) {
                    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        seccionCarrito(estado, viewModel)
                    }
                }
            }
        } else {
            // Celular: carrito arriba y buscador debajo, en una sola lista.
            LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                seccionCarrito(estado, viewModel)
                item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                seccionBuscador(estado, viewModel)
            }
        }
    }

    estado.ventaRegistrada?.let { venta ->
        AlertDialog(
            onDismissRequest = viewModel::cerrarVenta,
            title = { Text("Venta ${venta.numero} registrada") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Total: ${venta.total.soles()} · ${venta.medioPago}", fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    venta.detalles.forEach { d ->
                        Text("${d.cantidad} x ${d.productoNombre}")
                        Text(
                            "   Lote ${d.numeroLote} (vence ${d.fechaVencimiento}) · ${d.subtotal.soles()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                    Text(
                        "El stock se descontó primero de los lotes que vencen antes (FEFO).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = { Button(onClick = viewModel::cerrarVenta) { Text("Nueva venta") } }
        )
    }
}

private fun LazyListScope.seccionBuscador(estado: NuevaVentaUiState, viewModel: NuevaVentaViewModel) {
    item { Titulo("Agregar productos") }
    item {
        OutlinedTextField(
            value = estado.texto,
            onValueChange = viewModel::cambiarTexto,
            label = { Text("Buscar producto") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
    if (estado.buscando && estado.resultados.isEmpty()) {
        item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
    }
    items(estado.resultados, key = { "p-${it.id}" }) { p ->
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.nombre, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${p.precioVenta.soles()} · disponible: ${p.stockDisponible}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = { viewModel.agregar(p) }, enabled = p.stockDisponible > 0) {
                    Text(if (p.stockDisponible > 0) "Agregar" else "Sin stock")
                }
            }
        }
    }
}

private fun LazyListScope.seccionCarrito(estado: NuevaVentaUiState, viewModel: NuevaVentaViewModel) {
    item { Titulo("Venta actual") }
    if (estado.carrito.isEmpty()) {
        item {
            Text("Todavía no hay productos. Búsquelos y presione Agregar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    items(estado.carrito, key = { "c-${it.producto.id}" }) { item ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.producto.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${item.producto.precioVenta.soles()} c/u · ${item.subtotal.soles()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = { viewModel.cambiarCantidad(item.producto, -1) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(36.dp)) { Text("-") }
            Text(item.cantidad.toString(), Modifier.padding(horizontal = 10.dp), fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { viewModel.agregar(item.producto) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(36.dp)) { Text("+") }
            TextButton(onClick = { viewModel.quitar(item.producto) }) { Text("Quitar") }
        }
    }
    item { HorizontalDivider() }
    item {
        Text("Total: ${estado.total.soles()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
    item {
        Text("Medio de pago", style = MaterialTheme.typography.labelLarge)
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            MedioPago.entries.take(3).forEach { m ->
                FilterChip(selected = estado.medioPago == m, onClick = { viewModel.elegirMedio(m) }, label = { Text(m.etiqueta) })
            }
        }
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            MedioPago.entries.drop(3).forEach { m ->
                FilterChip(selected = estado.medioPago == m, onClick = { viewModel.elegirMedio(m) }, label = { Text(m.etiqueta) })
            }
        }
    }
    estado.error?.let { mensaje ->
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(mensaje, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                    TextButton(onClick = viewModel::cerrarError) { Text("Cerrar") }
                }
            }
        }
    }
    item {
        Button(
            onClick = viewModel::registrar,
            enabled = estado.carrito.isNotEmpty() && !estado.registrando,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(if (estado.registrando) "Registrando…" else "Registrar venta")
        }
    }
}
