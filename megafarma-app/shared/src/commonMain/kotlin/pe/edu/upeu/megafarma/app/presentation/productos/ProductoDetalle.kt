package pe.edu.upeu.megafarma.app.presentation.productos

import pe.edu.upeu.megafarma.app.presentation.componentes.Tarjeta
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
import pe.edu.upeu.megafarma.app.core.nuevaClave
import pe.edu.upeu.megafarma.app.core.soles
import pe.edu.upeu.megafarma.app.domain.model.Categoria
import pe.edu.upeu.megafarma.app.domain.model.DatosProducto
import pe.edu.upeu.megafarma.app.domain.model.Lote
import pe.edu.upeu.megafarma.app.domain.model.NuevaEntrada
import pe.edu.upeu.megafarma.app.domain.model.Permiso
import pe.edu.upeu.megafarma.app.domain.model.Producto
import pe.edu.upeu.megafarma.app.domain.model.Sesion
import pe.edu.upeu.megafarma.app.domain.model.StockProducto
import pe.edu.upeu.megafarma.app.domain.repository.InventarioRepository
import pe.edu.upeu.megafarma.app.domain.repository.ProductoRepository
import pe.edu.upeu.megafarma.app.domain.usecase.RegistrarEntradaUseCase
import pe.edu.upeu.megafarma.app.presentation.componentes.Aviso
import pe.edu.upeu.megafarma.app.presentation.componentes.Campo
import pe.edu.upeu.megafarma.app.presentation.componentes.Cargando
import pe.edu.upeu.megafarma.app.presentation.componentes.Etiqueta
import pe.edu.upeu.megafarma.app.presentation.componentes.MensajeError
import pe.edu.upeu.megafarma.app.presentation.componentes.TarjetaDato
import pe.edu.upeu.megafarma.app.presentation.componentes.Titulo
import pe.edu.upeu.megafarma.app.presentation.componentes.Vacio
import pe.edu.upeu.megafarma.app.presentation.tema.ColoresEstado

/* Dialogos que puede abrir la pantalla de detalle. */
sealed interface DialogoDetalle {
    data object Entrada : DialogoDetalle
    data object Editar : DialogoDetalle
    data class Baja(val lote: Lote) : DialogoDetalle
}

data class DetalleUiState(
    val producto: Producto? = null,
    val cargando: Boolean = true,
    val stock: StockProducto? = null,
    val error: String? = null,
    val aviso: String? = null,
    val avisoEsError: Boolean = false,
    val dialogo: DialogoDetalle? = null,
    val guardando: Boolean = false,
    val errorDialogo: String? = null,
    val categorias: List<Categoria> = emptyList()
)

/*
 * Detalle de un producto: stock por lote (HU-06), entradas (HU-05), baja de
 * lotes vencidos (HU-16), edicion (HU-02) y desactivacion (HU-04).
 */
class ProductoDetalleViewModel(
    private val inventario: InventarioRepository,
    private val productos: ProductoRepository,
    private val registrarEntradaUseCase: RegistrarEntradaUseCase
) : ViewModel() {

    private val _estado = MutableStateFlow(DetalleUiState())
    val estado: StateFlow<DetalleUiState> = _estado.asStateFlow()

    /* Clave de la entrada en curso: si se reintenta el envio, el backend no la duplica. */
    private var claveEntrada = nuevaClave()

    fun iniciar(producto: Producto) {
        if (_estado.value.producto?.id != producto.id) {
            _estado.value = DetalleUiState(producto = producto)
        }
        cargarStock()
    }

    fun cargarStock() {
        val id = _estado.value.producto?.id ?: return
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val r = inventario.stock(id)) {
                is Resultado.Exito -> _estado.update { it.copy(cargando = false, stock = r.datos) }
                is Resultado.Error -> _estado.update { it.copy(cargando = false, error = r.mensaje) }
            }
        }
    }

    fun abrirEntrada() {
        claveEntrada = nuevaClave()
        _estado.update { it.copy(dialogo = DialogoDetalle.Entrada, errorDialogo = null) }
    }

    fun abrirBaja(lote: Lote) = _estado.update { it.copy(dialogo = DialogoDetalle.Baja(lote), errorDialogo = null) }

    fun abrirEditar() {
        _estado.update { it.copy(dialogo = DialogoDetalle.Editar, errorDialogo = null) }
        if (_estado.value.categorias.isEmpty()) {
            viewModelScope.launch {
                (productos.categorias() as? Resultado.Exito)?.let { r -> _estado.update { it.copy(categorias = r.datos) } }
            }
        }
    }

    fun cerrarDialogo() = _estado.update { it.copy(dialogo = null, errorDialogo = null) }

    fun cerrarAviso() = _estado.update { it.copy(aviso = null) }

    fun registrarEntrada(numeroLote: String, fecha: String, cantidad: String, precioCompra: String) {
        val id = _estado.value.producto?.id ?: return
        val entrada = NuevaEntrada(
            productoId = id,
            numeroLote = numeroLote,
            fechaVencimiento = fecha,
            cantidad = cantidad.toIntOrNull() ?: 0,
            precioCompra = precioCompra.replace(',', '.').toDoubleOrNull()
        )
        ejecutar(
            accion = { registrarEntradaUseCase(entrada, claveEntrada) },
            exito = { lote -> "Entrada registrada: el lote ${lote.numeroLote} ahora tiene ${lote.cantidadActual} unidades" }
        )
    }

    fun darDeBaja(lote: Lote, motivo: String) {
        ejecutar(
            accion = { inventario.darDeBaja(lote.id, motivo.ifBlank { "Lote vencido" }) },
            exito = { l -> "Lote ${l.numeroLote} dado de baja" }
        )
    }

    fun guardarProducto(datos: DatosProducto) {
        val id = _estado.value.producto?.id ?: return
        _estado.update { it.copy(guardando = true, errorDialogo = null) }
        viewModelScope.launch {
            when (val r = productos.guardar(id, datos)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(guardando = false, dialogo = null, producto = r.datos, aviso = "Producto actualizado", avisoEsError = false)
                }
                is Resultado.Error -> _estado.update { it.copy(guardando = false, errorDialogo = r.mensaje) }
            }
        }
    }

    fun cambiarActivo() {
        val producto = _estado.value.producto ?: return
        viewModelScope.launch {
            val r = if (producto.activo) productos.desactivar(producto.id) else productos.activar(producto.id)
            when (r) {
                is Resultado.Exito -> _estado.update {
                    it.copy(
                        producto = producto.copy(activo = !producto.activo),
                        aviso = if (producto.activo) "Producto desactivado: ya no se vende ni recibe mercadería" else "Producto reactivado",
                        avisoEsError = false
                    )
                }
                is Resultado.Error -> _estado.update { it.copy(aviso = r.mensaje, avisoEsError = true) }
            }
        }
    }

    private fun <T> ejecutar(accion: suspend () -> Resultado<T>, exito: (T) -> String) {
        _estado.update { it.copy(guardando = true, errorDialogo = null) }
        viewModelScope.launch {
            when (val r = accion()) {
                is Resultado.Exito -> {
                    _estado.update { it.copy(guardando = false, dialogo = null, aviso = exito(r.datos), avisoEsError = false) }
                    cargarStock()
                }
                is Resultado.Error -> _estado.update { it.copy(guardando = false, errorDialogo = r.mensaje) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductoDetalleScreen(
    productoInicial: Producto,
    sesion: Sesion,
    onVolver: () -> Unit,
    viewModel: ProductoDetalleViewModel = koinViewModel()
) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(productoInicial.id) { viewModel.iniciar(productoInicial) }

    val producto = estado.producto ?: productoInicial
    val puedeInventario = sesion.puede(Permiso.INVENTARIO_ESCRIBIR)
    val puedeCatalogo = sesion.puede(Permiso.CATALOGO_ESCRIBIR)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Tarjeta(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(producto.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${producto.codigo} · ${producto.categoriaNombre}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                producto.principioActivo?.let { Text("Principio activo: $it") }
                producto.presentacion?.let { Text("Presentación: $it") }
                producto.laboratorio?.let { Text("Laboratorio: $it") }
                Text("Precio de venta: ${producto.precioVenta.soles()}", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (producto.requiereReceta) Etiqueta("Con receta", ColoresEstado.neutro)
                    if (!producto.activo) Etiqueta("Desactivado", ColoresEstado.peligro)
                }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (puedeInventario && producto.activo) {
                Button(onClick = viewModel::abrirEntrada) { Text("Registrar entrada por lote") }
            }
            if (puedeCatalogo) {
                OutlinedButton(onClick = viewModel::abrirEditar) { Text("Editar") }
                OutlinedButton(onClick = viewModel::cambiarActivo) { Text(if (producto.activo) "Desactivar" else "Reactivar") }
            }
            TextButton(onClick = onVolver) { Text("Volver a la lista") }
        }

        estado.aviso?.let { Aviso(it, estado.avisoEsError, viewModel::cerrarAviso) }

        when {
            estado.cargando && estado.stock == null -> Cargando()
            estado.error != null -> MensajeError(estado.error!!) { viewModel.cargarStock() }
            estado.stock != null -> {
                val stock = estado.stock!!
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TarjetaDato(
                        "Disponible para vender", stock.stockDisponible.toString(),
                        if (stock.bajoMinimo) ColoresEstado.alerta else ColoresEstado.ok,
                        Modifier.widthIn(min = 170.dp), "Sin lotes vencidos"
                    )
                    TarjetaDato("Registrado", stock.stockRegistrado.toString(), MaterialTheme.colorScheme.onSurface, Modifier.widthIn(min = 170.dp), "Incluye vencidos sin retirar")
                    TarjetaDato("Stock mínimo", stock.stockMinimo.toString(), MaterialTheme.colorScheme.onSurface, Modifier.widthIn(min = 170.dp), if (stock.bajoMinimo) "Hay que reponer" else "Nivel correcto")
                }

                Titulo("Lotes (ordenados por vencimiento)")
                Text(
                    "Al vender, el sistema descuenta primero del lote que vence antes (FEFO) y nunca de un lote vencido.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (stock.lotes.isEmpty()) {
                    Vacio("Este producto todavía no tiene entradas")
                } else {
                    val primeroFefo = stock.lotes.firstOrNull { !it.vencido && !it.dadoDeBaja && it.cantidadActual > 0 }?.id
                    stock.lotes.forEach { lote ->
                        TarjetaLote(
                            lote = lote,
                            saleprimero = lote.id == primeroFefo,
                            puedeDarDeBaja = puedeInventario && lote.vencido && !lote.dadoDeBaja && lote.cantidadActual > 0,
                            onBaja = { viewModel.abrirBaja(lote) }
                        )
                    }
                }
            }
        }
    }

    when (val dialogo = estado.dialogo) {
        DialogoDetalle.Entrada -> EntradaDialog(
            guardando = estado.guardando,
            error = estado.errorDialogo,
            onGuardar = viewModel::registrarEntrada,
            onCerrar = viewModel::cerrarDialogo
        )
        DialogoDetalle.Editar -> ProductoFormDialog(
            inicial = producto,
            categorias = estado.categorias,
            guardando = estado.guardando,
            error = estado.errorDialogo,
            onGuardar = viewModel::guardarProducto,
            onCerrar = viewModel::cerrarDialogo
        )
        is DialogoDetalle.Baja -> BajaDialog(
            lote = dialogo.lote,
            guardando = estado.guardando,
            error = estado.errorDialogo,
            onConfirmar = { motivo -> viewModel.darDeBaja(dialogo.lote, motivo) },
            onCerrar = viewModel::cerrarDialogo
        )
        null -> Unit
    }
}

@Composable
private fun TarjetaLote(lote: Lote, saleprimero: Boolean, puedeDarDeBaja: Boolean, onBaja: () -> Unit) {
    Tarjeta(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Lote ${lote.numeroLote}", fontWeight = FontWeight.SemiBold)
                val dias = when {
                    lote.diasParaVencer < 0 -> "venció hace ${-lote.diasParaVencer} días"
                    lote.diasParaVencer == 0L -> "vence hoy"
                    else -> "en ${lote.diasParaVencer} días"
                }
                Text("Vence: ${lote.fechaVencimiento} ($dias)", style = MaterialTheme.typography.bodySmall)
                Text("Cantidad: ${lote.cantidadActual} de ${lote.cantidadInicial}", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    when {
                        lote.dadoDeBaja -> Etiqueta("Dado de baja", ColoresEstado.neutro)
                        lote.vencido -> Etiqueta("Vencido: no se vende", ColoresEstado.peligro)
                        lote.diasParaVencer <= 60 -> Etiqueta("Por vencer", ColoresEstado.alerta)
                        else -> Etiqueta("Vigente", ColoresEstado.ok)
                    }
                    if (saleprimero) Etiqueta("Sale primero (FEFO)", MaterialTheme.colorScheme.secondary)
                }
            }
            if (puedeDarDeBaja) {
                OutlinedButton(onClick = onBaja) { Text("Dar de baja") }
            }
        }
    }
}

@Composable
private fun EntradaDialog(
    guardando: Boolean,
    error: String?,
    onGuardar: (String, String, String, String) -> Unit,
    onCerrar: () -> Unit
) {
    var lote by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!guardando) onCerrar() },
        title = { Text("Registrar entrada por lote") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Campo(lote, { lote = it }, "Número de lote *")
                Campo(fecha, { fecha = it }, "Fecha de vencimiento *", ayuda = "Formato AAAA-MM-DD, posterior a hoy")
                Campo(cantidad, { cantidad = it }, "Cantidad *", numerico = true)
                Campo(precio, { precio = it }, "Precio de compra (opcional)", numerico = true)
                Text(
                    "Si el lote ya existe con la misma fecha, se suma la cantidad.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = { onGuardar(lote, fecha, cantidad, precio) }, enabled = !guardando) {
                Text(if (guardando) "Guardando…" else "Registrar")
            }
        },
        dismissButton = { TextButton(onClick = onCerrar, enabled = !guardando) { Text("Cancelar") } }
    )
}

@Composable
private fun BajaDialog(
    lote: Lote,
    guardando: Boolean,
    error: String?,
    onConfirmar: (String) -> Unit,
    onCerrar: () -> Unit
) {
    var motivo by remember { mutableStateOf("Lote vencido retirado del estante") }
    AlertDialog(
        onDismissRequest = { if (!guardando) onCerrar() },
        title = { Text("Dar de baja el lote ${lote.numeroLote}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Se retirarán ${lote.cantidadActual} unidades vencidas del stock. Quedará registrado en el kardex.")
                Campo(motivo, { motivo = it }, "Motivo")
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirmar(motivo) }, enabled = !guardando) {
                Text(if (guardando) "Procesando…" else "Dar de baja")
            }
        },
        dismissButton = { TextButton(onClick = onCerrar, enabled = !guardando) { Text("Cancelar") } }
    )
}
