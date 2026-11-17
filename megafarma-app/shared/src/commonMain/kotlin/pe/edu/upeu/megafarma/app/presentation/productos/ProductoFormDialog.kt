package pe.edu.upeu.megafarma.app.presentation.productos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.edu.upeu.megafarma.app.domain.model.Categoria
import pe.edu.upeu.megafarma.app.domain.model.DatosProducto
import pe.edu.upeu.megafarma.app.domain.model.Producto
import pe.edu.upeu.megafarma.app.presentation.componentes.Campo

/* Formulario para registrar (inicial = null) o editar un producto. */
@Composable
fun ProductoFormDialog(
    inicial: Producto?,
    categorias: List<Categoria>,
    guardando: Boolean,
    error: String?,
    onGuardar: (DatosProducto) -> Unit,
    onCerrar: () -> Unit
) {
    var codigo by remember(inicial) { mutableStateOf(inicial?.codigo ?: "") }
    var nombre by remember(inicial) { mutableStateOf(inicial?.nombre ?: "") }
    var principio by remember(inicial) { mutableStateOf(inicial?.principioActivo ?: "") }
    var presentacion by remember(inicial) { mutableStateOf(inicial?.presentacion ?: "") }
    var laboratorio by remember(inicial) { mutableStateOf(inicial?.laboratorio ?: "") }
    var precio by remember(inicial) { mutableStateOf(inicial?.precioVenta?.toString() ?: "") }
    var stockMinimo by remember(inicial) { mutableStateOf(inicial?.stockMinimo?.toString() ?: "0") }
    var receta by remember(inicial) { mutableStateOf(inicial?.requiereReceta ?: false) }
    var categoriaId by remember(inicial) { mutableStateOf(inicial?.categoriaId) }
    var menuCategorias by remember { mutableStateOf(false) }
    var errorLocal by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!guardando) onCerrar() },
        title = { Text(if (inicial == null) "Nuevo producto" else "Editar producto") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Campo(codigo, { codigo = it }, "Código *")
                Campo(nombre, { nombre = it }, "Nombre *")
                Campo(principio, { principio = it }, "Principio activo")
                Campo(presentacion, { presentacion = it }, "Presentación")
                Campo(laboratorio, { laboratorio = it }, "Laboratorio")
                Campo(precio, { precio = it }, "Precio de venta (S/) *", numerico = true)
                Campo(stockMinimo, { stockMinimo = it }, "Stock mínimo *", numerico = true)

                Box {
                    OutlinedButton(onClick = { menuCategorias = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(categorias.firstOrNull { it.id == categoriaId }?.nombre ?: "Elegir categoría *")
                    }
                    DropdownMenu(expanded = menuCategorias, onDismissRequest = { menuCategorias = false }) {
                        categorias.forEach { c ->
                            DropdownMenuItem(text = { Text(c.nombre) }, onClick = {
                                categoriaId = c.id
                                menuCategorias = false
                            })
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = receta, onCheckedChange = { receta = it })
                    Text("Requiere receta médica")
                }

                val mensaje = errorLocal ?: error
                if (mensaje != null) Text(mensaje, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(
                enabled = !guardando,
                onClick = {
                    val precioNum = precio.replace(',', '.').toDoubleOrNull()
                    val minimoNum = stockMinimo.toIntOrNull()
                    val categoria = categoriaId
                    errorLocal = when {
                        codigo.isBlank() || nombre.isBlank() -> "Código y nombre son obligatorios"
                        precioNum == null || precioNum <= 0 -> "Ingrese un precio mayor que cero"
                        minimoNum == null || minimoNum < 0 -> "El stock mínimo debe ser 0 o más"
                        categoria == null -> "Elija una categoría"
                        else -> null
                    }
                    if (errorLocal == null && precioNum != null && minimoNum != null && categoria != null) {
                        onGuardar(
                            DatosProducto(
                                codigo = codigo.trim(),
                                nombre = nombre.trim(),
                                principioActivo = principio.ifBlank { null },
                                presentacion = presentacion.ifBlank { null },
                                laboratorio = laboratorio.ifBlank { null },
                                precioVenta = precioNum,
                                stockMinimo = minimoNum,
                                requiereReceta = receta,
                                categoriaId = categoria
                            )
                        )
                    }
                }
            ) { Text(if (guardando) "Guardando…" else "Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onCerrar, enabled = !guardando) { Text("Cancelar") }
        }
    )
}
