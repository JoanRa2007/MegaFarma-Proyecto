package pe.edu.upeu.megafarma.app.presentation.productos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.megafarma.app.core.TAMANIO_TABLA
import pe.edu.upeu.megafarma.app.core.rangoMostrado
import pe.edu.upeu.megafarma.app.core.soles
import pe.edu.upeu.megafarma.app.domain.model.Permiso
import pe.edu.upeu.megafarma.app.domain.model.Producto
import pe.edu.upeu.megafarma.app.domain.model.Sesion
import pe.edu.upeu.megafarma.app.presentation.componentes.Aviso
import pe.edu.upeu.megafarma.app.presentation.componentes.BotonAccion
import pe.edu.upeu.megafarma.app.presentation.componentes.Cargando
import pe.edu.upeu.megafarma.app.presentation.componentes.Columna
import pe.edu.upeu.megafarma.app.presentation.componentes.EncabezadoTabla
import pe.edu.upeu.megafarma.app.presentation.componentes.Etiqueta
import pe.edu.upeu.megafarma.app.presentation.componentes.FilaTabla
import pe.edu.upeu.megafarma.app.presentation.componentes.Iconos
import pe.edu.upeu.megafarma.app.presentation.componentes.MensajeError
import pe.edu.upeu.megafarma.app.presentation.componentes.Pastilla
import pe.edu.upeu.megafarma.app.presentation.componentes.Paginador
import pe.edu.upeu.megafarma.app.presentation.componentes.Tabla
import pe.edu.upeu.megafarma.app.presentation.componentes.Vacio
import pe.edu.upeu.megafarma.app.presentation.tema.ColoresEstado

/*
 * Catalogo de productos con su stock, en una tabla con acciones por fila
 * (editar, desactivar o reactivar). Al tocar una fila se abre el detalle
 * con el stock por lote.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductosScreen(
    sesion: Sesion,
    onAbrir: (Producto) -> Unit,
    viewModel: ProductosViewModel = koinViewModel()
) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargar() }
    val puedeEscribir = sesion.puede(Permiso.CATALOGO_ESCRIBIR)

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = estado.texto,
            onValueChange = viewModel::cambiarTexto,
            placeholder = { Text("Buscar por nombre, código o principio activo") },
            leadingIcon = { Icon(Iconos.Buscar, null, Modifier.size(18.dp)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // El filtro se aplica en el servidor; la pagina siempre trae hasta 10 filas.
        val filtro = estado.filtro
        if (puedeEscribir) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FiltroEstado.entries.forEach { f ->
                        FilterChip(selected = filtro == f, onClick = { viewModel.filtrar(f) }, label = { Text(f.etiqueta) })
                    }
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = viewModel::abrirFormulario, contentPadding = PaddingValues(horizontal = 14.dp)) {
                    Icon(Iconos.Agregar, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Nuevo")
                }
            }
        }

        estado.aviso?.let { Aviso(it, esError = estado.avisoEsError, onCerrar = viewModel::cerrarAviso) }

        when {
            estado.cargando && estado.productos.isEmpty() -> Cargando()
            estado.error != null -> MensajeError(estado.error!!) { viewModel.cargar() }
            estado.productos.isEmpty() -> Vacio(
                if (filtro == FiltroEstado.DESACTIVADOS && estado.texto.isBlank()) "No hay productos desactivados"
                else "No se encontraron productos"
            )
            else -> {
                Text(
                    "Toque una fila para ver su stock por lote",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Tabla(Modifier.weight(1f)) {
                    EncabezadoTabla(
                        listOf(Columna("Producto", 3f), Columna("Precio", 1f), Columna("Stock", 0.8f)) +
                            if (puedeEscribir) listOf(Columna("", 0.9f)) else emptyList()
                    )
                    LazyColumn {
                        items(estado.productos, key = { it.id }) { producto ->
                            FilaProducto(
                                producto = producto,
                                puedeEscribir = puedeEscribir,
                                onAbrir = { onAbrir(producto) },
                                onEditar = { viewModel.abrirEditar(producto) },
                                onDesactivar = { viewModel.pedirDesactivar(producto) },
                                onReactivar = { viewModel.reactivar(producto) }
                            )
                        }
                    }
                }
                Paginador(
                    pagina = estado.pagina,
                    totalPaginas = estado.totalPaginas,
                    onAnterior = viewModel::paginaAnterior,
                    onSiguiente = viewModel::paginaSiguiente,
                    detalle = rangoMostrado(estado.pagina, TAMANIO_TABLA, estado.productos.size, estado.totalElementos)
                )
            }
        }
    }

    if (estado.formularioAbierto) {
        ProductoFormDialog(
            inicial = estado.editando,
            categorias = estado.categorias,
            guardando = estado.guardando,
            error = estado.errorFormulario,
            onGuardar = viewModel::guardar,
            onCerrar = viewModel::cerrarFormulario
        )
    }

    estado.porDesactivar?.let { producto ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarDesactivar,
            title = { Text("¿Desactivar ${producto.nombre}?") },
            text = { Text("No se borra: deja de venderse y de recibir mercadería. Puede reactivarse cuando quiera.") },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmarDesactivar,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Desactivar") }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelarDesactivar) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun FilaProducto(
    producto: Producto,
    puedeEscribir: Boolean,
    onAbrir: () -> Unit,
    onEditar: () -> Unit,
    onDesactivar: () -> Unit,
    onReactivar: () -> Unit
) {
    val bajo = producto.stockDisponible <= producto.stockMinimo
    val colorStock = when {
        producto.stockDisponible <= 0 -> ColoresEstado.peligro
        bajo -> ColoresEstado.alerta
        else -> ColoresEstado.info
    }
    FilaTabla(onClick = onAbrir) {
        Column(Modifier.weight(3f).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                producto.nombre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOfNotNull(producto.codigo, producto.categoriaNombre, producto.laboratorio).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (producto.requiereReceta || !producto.activo || (bajo && producto.activo)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!producto.activo) Etiqueta("Desactivado", ColoresEstado.peligro)
                    if (producto.requiereReceta) Etiqueta("Con receta", ColoresEstado.neutro)
                    if (bajo && producto.activo) Etiqueta("Stock bajo", ColoresEstado.alerta, Iconos.Advertencia)
                }
            }
        }
        Text(
            producto.precioVenta.soles(),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Box(Modifier.weight(0.8f)) { Pastilla(producto.stockDisponible.toString(), colorStock) }
        if (puedeEscribir) {
            Row(Modifier.weight(0.9f), verticalAlignment = Alignment.CenterVertically) {
                BotonAccion(Iconos.Editar, "Editar", ColoresEstado.alerta, onEditar)
                if (producto.activo) {
                    BotonAccion(Iconos.Eliminar, "Desactivar", ColoresEstado.peligro, onDesactivar)
                } else {
                    BotonAccion(Iconos.Restaurar, "Reactivar", ColoresEstado.ok, onReactivar)
                }
            }
        }
    }
}
