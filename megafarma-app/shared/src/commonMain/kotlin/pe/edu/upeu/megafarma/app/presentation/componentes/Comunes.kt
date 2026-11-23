package pe.edu.upeu.megafarma.app.presentation.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.edu.upeu.megafarma.app.core.fechaCorta
import pe.edu.upeu.megafarma.app.core.fechaDesdeMillis
import pe.edu.upeu.megafarma.app.core.millisDesdeFecha

/* Piezas de interfaz reutilizadas por todas las pantallas. */

@Composable
fun Cargando(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(strokeWidth = 3.dp)
    }
}

@Composable
fun MensajeError(mensaje: String, onReintentar: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Iconos.Advertencia, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(mensaje, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
        if (onReintentar != null) {
            TextButton(onClick = onReintentar) { Text("Reintentar", color = MaterialTheme.colorScheme.error) }
        }
    }
}

/* Aviso que se puede cerrar: verde si salio bien, rojo si hubo un error. */
@Composable
fun Aviso(texto: String, esError: Boolean, onCerrar: () -> Unit) {
    val fondo = if (esError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val color = if (esError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(fondo)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (esError) Iconos.Advertencia else Iconos.Ok, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(texto, color = color, modifier = Modifier.weight(1f))
        TextButton(onClick = onCerrar) { Text("Cerrar", color = color) }
    }
}

/* Estado vacio con icono, en vez de un texto suelto. */
@Composable
fun Vacio(texto: String) {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Insignia(Iconos.Buscar, MaterialTheme.colorScheme.onSurfaceVariant, 56)
        Spacer(Modifier.height(12.dp))
        Text(texto, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/* Etiqueta pequena de color con borde (por ejemplo "Vencido", "vence en 9 dias"). */
@Composable
fun Etiqueta(texto: String, color: Color, icono: ImageVector? = null) {
    Row(
        Modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icono != null) {
            Icon(icono, null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(texto, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}

/* Recuadro con un valor corto (stock, cantidades) centrado, como en las tablas de inventario. */
@Composable
fun Pastilla(texto: String, color: Color) {
    Box(
        Modifier
            .widthIn(min = 32.dp)
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(texto, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

/* Boton de icono pequeno para las acciones de una fila (editar, eliminar...). */
@Composable
fun BotonAccion(icono: ImageVector, descripcion: String, color: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(icono, contentDescription = descripcion, tint = color, modifier = Modifier.size(18.dp))
    }
}

/* Tarjeta con un numero grande y una franja de color: se usa en reportes y dashboard. */
@Composable
fun TarjetaDato(
    titulo: String,
    valor: String,
    color: Color,
    modifier: Modifier = Modifier,
    detalle: String? = null,
    onClick: (() -> Unit)? = null
) {
    val base = modifier
        .clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
    Row((if (onClick != null) base.clickable { onClick() } else base).height(IntrinsicSize.Min)) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(color))
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(valor, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            if (detalle != null) {
                Spacer(Modifier.height(2.dp))
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = color)
            }
        }
    }
}

@Composable
fun Titulo(texto: String, modifier: Modifier = Modifier) {
    Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = modifier)
}

@Composable
fun Campo(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    numerico: Boolean = false,
    ayuda: String? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = true,
        supportingText = if (ayuda != null) ({ Text(ayuda) }) else null,
        keyboardOptions = if (numerico) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        modifier = modifier.fillMaxWidth()
    )
}

/* Barra horizontal proporcional para los graficos simples de los reportes. */
@Composable
fun BarraHorizontal(etiqueta: String, fraccion: Float, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, modifier = Modifier.width(110.dp), style = MaterialTheme.typography.bodyMedium)
        Box(Modifier.weight(1f).height(18.dp)) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraccion.coerceIn(0.02f, 1f))
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
            )
        }
        Text(valor, modifier = Modifier.padding(start = 8.dp).width(96.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

/*
 * Campo de fecha con calendario: se toca y se elige el dia, sin escribir "AAAA-MM-DD".
 * Guarda la fecha en el formato del backend y la muestra como 23/11/2026.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    ayuda: String? = null,
    textoVacio: String = "Elegir fecha"
) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = if (valor.isBlank()) "" else valor.fechaCorta(),
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            placeholder = { Text(textoVacio) },
            trailingIcon = { Icon(Iconos.Calendario, contentDescription = "Abrir calendario", modifier = Modifier.size(20.dp)) },
            supportingText = if (ayuda != null) ({ Text(ayuda) }) else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        // Capa transparente encima del campo: cualquier toque abre el calendario.
        Box(Modifier.matchParentSize().padding(top = 8.dp, bottom = if (ayuda != null) 22.dp else 0.dp).clickable { abierto = true })
    }
    if (abierto) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = millisDesdeFecha(valor))
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { onCambio(fechaDesdeMillis(it)) }
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                Row {
                    if (valor.isNotBlank()) TextButton(onClick = { onCambio(""); abierto = false }) { Text("Borrar") }
                    TextButton(onClick = { abierto = false }) { Text("Cancelar") }
                }
            }
        ) {
            DatePicker(state = estado, showModeToggle = false)
        }
    }
}

/* Controles de pagina compactos: caben en una fila tambien en el celular. */
@Composable
fun Paginador(pagina: Int, totalPaginas: Int, onAnterior: () -> Unit, onSiguiente: () -> Unit, detalle: String? = null) {
    // Siempre visible: con una sola pagina se ve "1 de 1" y los botones quedan deshabilitados.
    val paginas = totalPaginas.coerceAtLeast(1)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onAnterior, enabled = pagina > 0, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("‹ Anterior") }
            Text(
                "${pagina + 1} de $paginas",
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            OutlinedButton(onClick = onSiguiente, enabled = pagina + 1 < paginas, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Siguiente ›") }
        }
        if (detalle != null) {
            Text(detalle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
