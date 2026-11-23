package pe.edu.upeu.megafarma.app.presentation.ventas

import pe.edu.upeu.megafarma.app.presentation.componentes.Tarjeta
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
import pe.edu.upeu.megafarma.app.core.montoTexto
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

    /* Medios que aun no se usan: cada medio aparece una sola vez en el pago mixto. */
    val mediosLibres: List<MedioPago> get() = MedioPago.entries.filter { m -> pagos.none { it.medio == m } }

    /* Lo que falta segun los montos escritos (un monto vacio cuenta como cero). */
    val faltaEscrita: Double
        get() = (total - pagos.sumOf { it.monto.replace(',', '.').toDoubleOrNull() ?: 0.0 }).coerceAtLeast(0.0)

    /* Se puede sumar otro medio si hay productos, queda algo por pagar y quedan medios libres. */
    val puedeAgregarPago: Boolean get() = carrito.isNotEmpty() && faltaEscrita > 0.004 && mediosLibres.isNotEmpty()

    /* Solo el efectivo puede pasar el total (genera vuelto); los demas no. */
    val excesoSinEfectivo: Boolean
        get() = pagosAEnviar.filter { it.medio != MedioPago.EFECTIVO }.sumOf { it.monto } > total + 0.004

    val listoParaCobrar: Boolean get() = carrito.isNotEmpty() && falta <= 0.004 && !excesoSinEfectivo
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
    /*
     * Pago mixto: agrega UNA fila con un medio que todavia no se usa y el monto que falta.
     * Si ya no falta nada o no quedan medios, no hace nada (antes se agregaban filas sin limite).
     */
    fun agregarPago() = _estado.update { e ->
        if (!e.puedeAgregarPago) return@update e
        // La fila nueva ya trae lo que falta pagar; el cajero solo ajusta si hace falta.
        val restante = e.faltaEscrita
        // Lo mas comun para completar un pago: Yape, luego Plin, tarjeta, transferencia y efectivo.
        val preferencia = listOf(MedioPago.YAPE, MedioPago.PLIN, MedioPago.TARJETA, MedioPago.TRANSFERENCIA, MedioPago.EFECTIVO)
        val medio = preferencia.first { it in e.mediosLibres }
        e.copy(
            pagos = e.pagos + PagoEditable(siguientePago++, medio, restante.montoTexto()),
            error = null
        )
    }

    fun quitarPago(id: Int) = _estado.update { e ->
        e.copy(pagos = e.pagos.filter { it.id != id }.ifEmpty { listOf(PagoEditable(siguientePago++, MedioPago.EFECTIVO)) })
    }

    fun cambiarPago(id: Int, cambio: (PagoEditable) -> PagoEditable) = _estado.update { e ->
        e.copy(pagos = e.pagos.map { if (it.id == id) cambio(it) else it }, error = null)
    }

    /* Solo numeros con hasta 2 decimales en el monto. */
    fun cambiarMonto(id: Int, texto: String) {
        val limpio = texto.replace(',', '.').filter { it.isDigit() || it == '.' }
        val partes = limpio.split('.')
        val valido = partes.size <= 2 && (partes.getOrNull(1)?.length ?: 0) <= 2 && limpio.length <= 9
        if (valido) cambiarPago(id) { it.copy(monto = limpio) }
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
                Tarjeta(Modifier.weight(1.1f).fillMaxHeight()) {
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
        Tarjeta(Modifier.fillMaxWidth()) {
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
    item { Titulo("Pago") }
    items(estado.pagos, key = { "pago-${it.id}" }) { pago ->
        FilaPago(pago, unico = estado.pagos.size == 1, estado.mediosLibres, viewModel)
    }
    item {
        // Un medio por fila y solo mientras falte pagar: no se pueden repetir medios.
        OutlinedButton(onClick = viewModel::agregarPago, enabled = estado.puedeAgregarPago, modifier = Modifier.fillMaxWidth()) {
            Text(
                when {
                    estado.carrito.isEmpty() -> "Pago mixto: agregue productos primero"
                    estado.mediosLibres.isEmpty() -> "Ya se usaron todos los medios de pago"
                    estado.faltaEscrita <= 0.004 -> "El total ya está cubierto"
                    else -> "+ Pagar lo que falta con otro medio"
                }
            )
        }
    }
    if (estado.carrito.isNotEmpty()) {
        item { ResumenPago(estado) }
    }

    estado.error?.let { mensaje ->
        item {
            Tarjeta(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
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
            enabled = estado.listoParaCobrar && !estado.registrando,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(
                when {
                    estado.registrando -> "Registrando…"
                    estado.carrito.isEmpty() -> "Agregue productos para cobrar"
                    estado.falta > 0.004 -> "Falta pagar ${estado.falta.soles()}"
                    else -> "Cobrar ${estado.total.soles()} y emitir ${estado.tipoComprobante.etiqueta.lowercase()}"
                }
            )
        }
    }
}

/* Total, pagado y lo que falta o el vuelto, siempre a la vista antes de cobrar. */
@Composable
private fun ResumenPago(estado: NuevaVentaUiState) {
    Tarjeta(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FilaMonto("Total", estado.total.soles())
            FilaMonto("Pagado", estado.pagado.soles())
            when {
                estado.excesoSinEfectivo -> Text(
                    "Tarjeta, billeteras y transferencia no pueden pasar el total: solo el efectivo da vuelto.",
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall
                )
                estado.falta > 0.004 -> FilaMonto("Falta pagar", estado.falta.soles(), MaterialTheme.colorScheme.error, destacado = true)
                estado.vuelto > 0.004 -> FilaMonto("Vuelto", estado.vuelto.soles(), MaterialTheme.colorScheme.primary, destacado = true)
                else -> FilaMonto("Pago exacto", "✓", MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun FilaMonto(etiqueta: String, valor: String, color: androidx.compose.ui.graphics.Color? = null, destacado: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, Modifier.weight(1f), color = color ?: MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (destacado) FontWeight.SemiBold else FontWeight.Normal)
        Text(valor, color = color ?: MaterialTheme.colorScheme.onSurface,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.SemiBold,
            style = if (destacado) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge)
    }
}

/*
 * Una forma de pago. En el celular el medio y el monto van uno debajo del otro para que
 * no se aplasten; el menu solo ofrece los medios que aun no se usan en esta venta.
 */
@Composable
private fun FilaPago(pago: PagoEditable, unico: Boolean, libres: List<MedioPago>, viewModel: NuevaVentaViewModel) {
    var menu by remember { mutableStateOf(false) }
    val opciones = listOf(pago.medio) + libres
    val selector = @Composable {
        Box {
            OutlinedButton(onClick = { menu = true }) { Text(pago.medio.etiqueta + "  ▾") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                opciones.forEach { m ->
                    DropdownMenuItem(text = { Text(m.etiqueta) }, onClick = {
                        if (m != pago.medio) viewModel.cambiarPago(pago.id) { it.copy(medio = m, referencia = "") }
                        menu = false
                    })
                }
            }
        }
    }
    val monto = @Composable { m: Modifier ->
        Campo(
            pago.monto,
            { v -> viewModel.cambiarMonto(pago.id, v) },
            if (pago.medio == MedioPago.EFECTIVO) "Efectivo recibido" else "Monto",
            m,
            numerico = true,
            ayuda = if (unico && pago.medio == MedioPago.EFECTIVO) "Vacío = monto exacto" else null
        )
    }
    Tarjeta(Modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.padding(12.dp)) {
            val angosto = maxWidth < 380.dp
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (angosto) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        selector()
                        Spacer(Modifier.weight(1f))
                        if (!unico) TextButton(onClick = { viewModel.quitarPago(pago.id) }) { Text("Quitar") }
                    }
                    monto(Modifier)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        selector()
                        monto(Modifier.weight(1f))
                        if (!unico) TextButton(onClick = { viewModel.quitarPago(pago.id) }) { Text("Quitar") }
                    }
                }
                when (pago.medio) {
                    MedioPago.YAPE, MedioPago.PLIN, MedioPago.TRANSFERENCIA -> Campo(
                        pago.referencia, { v -> viewModel.cambiarPago(pago.id) { it.copy(referencia = v.filter { c -> c.isLetterOrDigit() || c == '-' }.take(30)) } },
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
