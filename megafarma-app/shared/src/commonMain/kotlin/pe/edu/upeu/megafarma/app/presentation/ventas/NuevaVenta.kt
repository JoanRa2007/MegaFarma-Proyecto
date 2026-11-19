package pe.edu.upeu.megafarma.app.presentation.ventas

import androidx.compose.foundation.clickable
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
import pe.edu.upeu.megafarma.app.domain.model.Cliente
import pe.edu.upeu.megafarma.app.domain.model.Comprobante
import pe.edu.upeu.megafarma.app.domain.model.DatosCliente
import pe.edu.upeu.megafarma.app.domain.model.LineaVenta
import pe.edu.upeu.megafarma.app.domain.model.MedioPago
import pe.edu.upeu.megafarma.app.domain.model.NuevaVenta
import pe.edu.upeu.megafarma.app.domain.model.NuevoPago
import pe.edu.upeu.megafarma.app.domain.model.Producto
import pe.edu.upeu.megafarma.app.domain.model.TipoComprobante
import pe.edu.upeu.megafarma.app.domain.model.Venta
import pe.edu.upeu.megafarma.app.domain.repository.ClienteRepository
import pe.edu.upeu.megafarma.app.domain.repository.ProductoRepository
import pe.edu.upeu.megafarma.app.domain.repository.VentaRepository
import pe.edu.upeu.megafarma.app.domain.usecase.RegistrarVentaUseCase
import pe.edu.upeu.megafarma.app.presentation.componentes.Campo
import pe.edu.upeu.megafarma.app.presentation.componentes.Titulo
import pe.edu.upeu.megafarma.app.presentation.comprobante.ComprobanteDialog

data class ItemCarrito(val producto: Producto, val cantidad: Int) {
    val subtotal: Double get() = producto.precioVenta * cantidad
}

/* Una forma de pago mientras se escribe (los montos son texto). */
data class PagoEditable(val id: Int, val medio: MedioPago, val monto: String = "", val referencia: String = "")

data class NuevaVentaUiState(
    // Productos
    val texto: String = "",
    val buscando: Boolean = false,
    val resultados: List<Producto> = emptyList(),
    val carrito: List<ItemCarrito> = emptyList(),
    // Cliente y comprobante
    val cliente: Cliente? = null,
    val tipoComprobante: TipoComprobante = TipoComprobante.BOLETA,
    val dialogoCliente: Boolean = false,
    val textoCliente: String = "",
    val clientes: List<Cliente> = emptyList(),
    val errorCliente: String? = null,
    // Pagos
    val pagos: List<PagoEditable> = listOf(PagoEditable(1, MedioPago.EFECTIVO)),
    // Envio y resultado
    val registrando: Boolean = false,
    val error: String? = null,
    val ventaRegistrada: Venta? = null,
    val comprobante: Comprobante? = null,
    val cargandoComprobante: Boolean = false,
    val errorComprobante: String? = null,
    val xml: String? = null
) {
    val total: Double get() = carrito.sumOf { it.subtotal }

    /* Con un solo pago en efectivo y sin monto, se asume pago exacto. */
    val pagosAEnviar: List<NuevoPago>
        get() = pagos.map { p ->
            val monto = p.monto.replace(',', '.').toDoubleOrNull()
                ?: if (pagos.size == 1) total else 0.0
            NuevoPago(p.medio, monto, p.referencia.ifBlank { null })
        }
    val pagado: Double get() = pagosAEnviar.sumOf { it.monto }
    val falta: Double get() = (total - pagado).coerceAtLeast(0.0)
    val vuelto: Double get() = (pagado - total).coerceAtLeast(0.0)
}

/*
 * Punto de venta (HU-09, HU-34): carrito, cliente, boleta o factura,
 * pagos mixtos con vuelto y comprobante electronico al terminar.
 */
class NuevaVentaViewModel(
    private val productos: ProductoRepository,
    private val clientes: ClienteRepository,
    private val registrarVenta: RegistrarVentaUseCase,
    private val ventas: VentaRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(NuevaVentaUiState())
    val estado: StateFlow<NuevaVentaUiState> = _estado.asStateFlow()

    private var busqueda: Job? = null
    private var siguientePago = 2

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

    // ---------- Carrito ----------
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

    // ---------- Comprobante y cliente ----------
    fun elegirComprobante(tipo: TipoComprobante) = _estado.update { it.copy(tipoComprobante = tipo, error = null) }

    fun abrirClientes() {
        _estado.update { it.copy(dialogoCliente = true, errorCliente = null) }
        buscarClientes()
    }

    fun cerrarClientes() = _estado.update { it.copy(dialogoCliente = false) }

    fun cambiarTextoCliente(texto: String) {
        _estado.update { it.copy(textoCliente = texto) }
        buscarClientes()
    }

    private fun buscarClientes() {
        viewModelScope.launch {
            when (val r = clientes.buscar(_estado.value.textoCliente)) {
                is Resultado.Exito -> _estado.update { it.copy(clientes = r.datos) }
                is Resultado.Error -> _estado.update { it.copy(errorCliente = r.mensaje) }
            }
        }
    }

    fun elegirCliente(cliente: Cliente?) = _estado.update {
        it.copy(
            cliente = cliente,
            dialogoCliente = false,
            // Un cliente con RUC normalmente pide factura.
            tipoComprobante = if (cliente?.tipoDocumento == "RUC") TipoComprobante.FACTURA else TipoComprobante.BOLETA,
            error = null
        )
    }

    fun registrarCliente(datos: DatosCliente) {
        viewModelScope.launch {
            when (val r = clientes.crear(datos)) {
                is Resultado.Exito -> elegirCliente(r.datos)
                is Resultado.Error -> _estado.update { it.copy(errorCliente = r.mensaje) }
            }
        }
    }

    // ---------- Pagos ----------
    fun agregarPago() = _estado.update {
        it.copy(pagos = it.pagos + PagoEditable(siguientePago++, MedioPago.YAPE), error = null)
    }

    fun quitarPago(id: Int) = _estado.update { e ->
        e.copy(pagos = e.pagos.filter { it.id != id }.ifEmpty { listOf(PagoEditable(siguientePago++, MedioPago.EFECTIVO)) })
    }

    fun cambiarPago(id: Int, cambio: (PagoEditable) -> PagoEditable) = _estado.update { e ->
        e.copy(pagos = e.pagos.map { if (it.id == id) cambio(it) else it }, error = null)
    }

    // ---------- Registro ----------
    fun cerrarError() = _estado.update { it.copy(error = null) }

    fun registrar() {
        val actual = _estado.value
        if (actual.registrando) return
        val venta = NuevaVenta(
            lineas = actual.carrito.map { LineaVenta(it.producto.id, it.cantidad) },
            pagos = actual.pagosAEnviar,
            tipoComprobante = actual.tipoComprobante,
            cliente = actual.cliente
        )
        _estado.update { it.copy(registrando = true, error = null) }
        viewModelScope.launch {
            when (val r = registrarVenta(venta, actual.total, clave)) {
                is Resultado.Exito -> {
                    clave = nuevaClave()
                    _estado.update {
                        it.copy(
                            registrando = false, carrito = emptyList(), ventaRegistrada = r.datos,
                            cliente = null, tipoComprobante = TipoComprobante.BOLETA,
                            pagos = listOf(PagoEditable(siguientePago++, MedioPago.EFECTIVO)),
                            comprobante = null, cargandoComprobante = true, errorComprobante = null
                        )
                    }
                    cargarComprobante(r.datos.id)
                    buscar() // el stock cambio
                }
                is Resultado.Error -> _estado.update { it.copy(registrando = false, error = r.mensaje) }
            }
        }
    }

    private suspend fun cargarComprobante(ventaId: Long) {
        when (val c = ventas.comprobante(ventaId)) {
            is Resultado.Exito -> _estado.update { it.copy(cargandoComprobante = false, comprobante = c.datos) }
            is Resultado.Error -> _estado.update { it.copy(cargandoComprobante = false, errorComprobante = c.mensaje) }
        }
    }

    fun verXml() {
        val venta = _estado.value.ventaRegistrada ?: return
        viewModelScope.launch {
            when (val r = ventas.comprobanteXml(venta.id)) {
                is Resultado.Exito -> _estado.update { it.copy(xml = r.datos) }
                is Resultado.Error -> _estado.update { it.copy(errorComprobante = r.mensaje) }
            }
        }
    }

    fun cerrarXml() = _estado.update { it.copy(xml = null) }

    fun cerrarVenta() = _estado.update { it.copy(ventaRegistrada = null, comprobante = null, xml = null) }
}

@Composable
fun NuevaVentaScreen(viewModel: NuevaVentaViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargar() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 840.dp) {
            // Pantalla ancha: buscador a la izquierda y la venta a la derecha.
            Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    seccionBuscador(estado, viewModel)
                }
                Card(Modifier.weight(1.1f).fillMaxHeight()) {
                    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        seccionVenta(estado, viewModel)
                    }
                }
            }
        } else {
            // Celular: la venta arriba y el buscador debajo, en una sola lista.
            LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                seccionVenta(estado, viewModel)
                item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                seccionBuscador(estado, viewModel)
            }
        }
    }

    if (estado.dialogoCliente) {
        ClienteDialog(estado, viewModel)
    }

    if (estado.ventaRegistrada != null) {
        ComprobanteDialog(
            comprobante = estado.comprobante,
            cargando = estado.cargandoComprobante,
            error = estado.errorComprobante,
            xml = estado.xml,
            onVerXml = viewModel::verXml,
            onCerrarXml = viewModel::cerrarXml,
            onCerrar = viewModel::cerrarVenta,
            textoBotonCerrar = "Nueva venta"
        )
    }
}

private fun LazyListScope.seccionBuscador(estado: NuevaVentaUiState, viewModel: NuevaVentaViewModel) {
    item { Titulo("Agregar productos") }
    item {
        OutlinedTextField(
            value = estado.texto,
            onValueChange = viewModel::cambiarTexto,
            label = { Text("Buscar por nombre, código o principio activo") },
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
                        "${p.precioVenta.soles()} · disponible: ${p.stockDisponible}" + if (p.requiereReceta) " · con receta" else "",
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

private fun LazyListScope.seccionVenta(estado: NuevaVentaUiState, viewModel: NuevaVentaViewModel) {
    // ---------- Cliente y comprobante ----------
    item { Titulo("Cliente y comprobante") }
    item {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val c = estado.cliente
                Text(c?.nombre ?: "Clientes varios", fontWeight = FontWeight.SemiBold)
                Text(
                    c?.let { "${it.tipoDocumento} ${it.numeroDocumento}" } ?: "Sin documento (boletas hasta S/ 700)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (estado.cliente != null) TextButton(onClick = { viewModel.elegirCliente(null) }) { Text("Quitar") }
            OutlinedButton(onClick = viewModel::abrirClientes) { Text(if (estado.cliente == null) "Elegir cliente" else "Cambiar") }
        }
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TipoComprobante.entries.forEach { t ->
                FilterChip(selected = estado.tipoComprobante == t, onClick = { viewModel.elegirComprobante(t) }, label = { Text(t.etiqueta) })
            }
        }
    }
    item { HorizontalDivider() }

    // ---------- Carrito ----------
    item { Titulo("Productos") }
    if (estado.carrito.isEmpty()) {
        item { Text("Todavía no hay productos. Búsquelos y presione Agregar.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
    item {
        Text("Total: ${estado.total.soles()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
    item { HorizontalDivider() }

    // ---------- Pagos ----------
    item {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Titulo("Pago", Modifier.weight(1f))
            TextButton(onClick = viewModel::agregarPago) { Text("+ Otro medio (pago mixto)") }
        }
    }
    items(estado.pagos, key = { "pago-${it.id}" }) { pago ->
        FilaPago(pago, unico = estado.pagos.size == 1, viewModel)
    }
    item {
        Column {
            Text("Pagado: ${estado.pagado.soles()}", style = MaterialTheme.typography.bodyMedium)
            if (estado.falta > 0.004 && estado.carrito.isNotEmpty()) {
                Text("Falta: ${estado.falta.soles()}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
            if (estado.vuelto > 0.004) {
                Text("Vuelto: ${estado.vuelto.soles()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium)
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
            Text(if (estado.registrando) "Registrando…" else "Cobrar y emitir ${estado.tipoComprobante.etiqueta.lowercase()}")
        }
    }
}

@Composable
private fun FilaPago(pago: PagoEditable, unico: Boolean, viewModel: NuevaVentaViewModel) {
    var menu by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                OutlinedButton(onClick = { menu = true }) { Text(pago.medio.etiqueta) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    MedioPago.entries.forEach { m ->
                        DropdownMenuItem(text = { Text(m.etiqueta) }, onClick = {
                            viewModel.cambiarPago(pago.id) { it.copy(medio = m, referencia = "") }
                            menu = false
                        })
                    }
                }
            }
            Campo(
                pago.monto,
                { v -> viewModel.cambiarPago(pago.id) { it.copy(monto = v) } },
                if (pago.medio == MedioPago.EFECTIVO) "Efectivo recibido" else "Monto",
                Modifier.weight(1f),
                numerico = true,
                ayuda = if (unico && pago.medio == MedioPago.EFECTIVO) "Vacío = monto exacto" else null
            )
            if (!unico) TextButton(onClick = { viewModel.quitarPago(pago.id) }) { Text("Quitar") }
        }
        when (pago.medio) {
            MedioPago.YAPE, MedioPago.PLIN, MedioPago.TRANSFERENCIA -> Campo(
                pago.referencia, { v -> viewModel.cambiarPago(pago.id) { it.copy(referencia = v) } },
                "N.° de operación de ${pago.medio.etiqueta}"
            )
            MedioPago.TARJETA -> Campo(
                pago.referencia, { v -> viewModel.cambiarPago(pago.id) { it.copy(referencia = v.filter { c -> c.isDigit() }.take(4)) } },
                "Últimos 4 dígitos de la tarjeta", numerico = true
            )
            MedioPago.EFECTIVO -> Unit
        }
    }
}

@Composable
private fun ClienteDialog(estado: NuevaVentaUiState, viewModel: NuevaVentaViewModel) {
    var tipo by remember { mutableStateOf("DNI") }
    var numero by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = viewModel::cerrarClientes,
        title = { Text("Cliente") },
        text = {
            Column(Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Campo(estado.textoCliente, viewModel::cambiarTextoCliente, "Buscar por nombre o documento")
                LazyColumn(Modifier.heightIn(max = 180.dp)) {
                    items(estado.clientes, key = { it.id }) { c ->
                        Row(Modifier.fillMaxWidth().clickable { viewModel.elegirCliente(c) }.padding(vertical = 8.dp)) {
                            Column {
                                Text(c.nombre, fontWeight = FontWeight.SemiBold)
                                Text("${c.tipoDocumento} ${c.numeroDocumento}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                HorizontalDivider()
                Text("Registrar cliente nuevo", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("DNI", "RUC", "CE").forEach { t ->
                        FilterChip(selected = tipo == t, onClick = { tipo = t }, label = { Text(t) })
                    }
                }
                Campo(numero, { numero = it }, if (tipo == "RUC") "RUC (11 dígitos)" else if (tipo == "DNI") "DNI (8 dígitos)" else "Carné de extranjería")
                Campo(nombre, { nombre = it }, if (tipo == "RUC") "Razón social" else "Nombre completo")
                if (estado.errorCliente != null) Text(estado.errorCliente, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.registrarCliente(DatosCliente(tipo, numero, nombre, null)) }) { Text("Registrar y elegir") }
        },
        dismissButton = { TextButton(onClick = viewModel::cerrarClientes) { Text("Cancelar") } }
    )
}
