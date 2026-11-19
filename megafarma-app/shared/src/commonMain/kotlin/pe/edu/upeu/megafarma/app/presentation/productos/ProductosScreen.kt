package pe.edu.upeu.megafarma.app.presentation.productos

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
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.megafarma.app.core.soles
import pe.edu.upeu.megafarma.app.domain.model.Permiso
import pe.edu.upeu.megafarma.app.domain.model.Producto
import pe.edu.upeu.megafarma.app.domain.model.Sesion
import pe.edu.upeu.megafarma.app.presentation.componentes.Aviso
import pe.edu.upeu.megafarma.app.presentation.componentes.Cargando
import pe.edu.upeu.megafarma.app.presentation.componentes.Etiqueta
import pe.edu.upeu.megafarma.app.presentation.componentes.MensajeError
import pe.edu.upeu.megafarma.app.presentation.componentes.Vacio
import pe.edu.upeu.megafarma.app.presentation.tema.ColoresEstado

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
            label = { Text("Buscar por nombre, código o principio activo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (puedeEscribir) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = estado.incluirInactivos, onCheckedChange = { viewModel.alternarInactivos() })
                Text("Ver desactivados", Modifier.padding(start = 8.dp))
                Spacer(Modifier.weight(1f))
                Button(onClick = viewModel::abrirFormulario) { Text("Nuevo producto") }
            }
        }

        estado.aviso?.let { Aviso(it, esError = false, onCerrar = viewModel::cerrarAviso) }

        when {
            estado.cargando && estado.productos.isEmpty() -> Cargando()
            estado.error != null -> MensajeError(estado.error!!) { viewModel.cargar() }
            estado.productos.isEmpty() -> Vacio("No se encontraron productos")
            else -> {
                Text(
                    "${estado.productos.size} producto(s) · toque uno para ver su stock por lote",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(estado.productos, key = { it.id }) { producto ->
                        TarjetaProducto(producto) { onAbrir(producto) }
                    }
                }
            }
        }
    }

    if (estado.formularioAbierto) {
        ProductoFormDialog(
            inicial = null,
            categorias = estado.categorias,
            guardando = estado.guardando,
            error = estado.errorFormulario,
            onGuardar = viewModel::guardar,
            onCerrar = viewModel::cerrarFormulario
        )
    }
}

@Composable
private fun TarjetaProducto(producto: Producto, onClick: () -> Unit) {
    val bajo = producto.stockDisponible <= producto.stockMinimo
    Tarjeta(Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(producto.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${producto.codigo} · ${producto.categoriaNombre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (producto.requiereReceta) Etiqueta("Con receta", ColoresEstado.neutro)
                    if (!producto.activo) Etiqueta("Desactivado", ColoresEstado.peligro)
                    if (bajo && producto.activo) Etiqueta("Stock bajo", ColoresEstado.alerta)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(producto.precioVenta.soles(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Stock: ${producto.stockDisponible}",
                    color = if (bajo) ColoresEstado.alerta else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
