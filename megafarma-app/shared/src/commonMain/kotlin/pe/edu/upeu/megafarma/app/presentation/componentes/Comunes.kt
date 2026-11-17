package pe.edu.upeu.megafarma.app.presentation.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/* Piezas de interfaz reutilizadas por todas las pantallas. */

@Composable
fun Cargando(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun MensajeError(mensaje: String, onReintentar: (() -> Unit)? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(mensaje, color = MaterialTheme.colorScheme.onErrorContainer)
            if (onReintentar != null) {
                TextButton(onClick = onReintentar) { Text("Reintentar") }
            }
        }
    }
}

/* Aviso que se puede cerrar: verde si salio bien, rojo si hubo un error. */
@Composable
fun Aviso(texto: String, esError: Boolean, onCerrar: () -> Unit) {
    val fondo = if (esError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val color = if (esError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Card(colors = CardDefaults.cardColors(containerColor = fondo), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(texto, color = color, modifier = Modifier.weight(1f))
            TextButton(onClick = onCerrar) { Text("Cerrar", color = color) }
        }
    }
}

@Composable
fun Vacio(texto: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(texto, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/* Etiqueta pequena de color (por ejemplo "Vencido", "Stock bajo"). */
@Composable
fun Etiqueta(texto: String, color: Color) {
    Box(
        Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(texto, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

/* Tarjeta con un numero grande: se usa en el inicio y en los reportes. */
@Composable
fun TarjetaDato(
    titulo: String,
    valor: String,
    color: Color,
    modifier: Modifier = Modifier,
    detalle: String? = null,
    onClick: (() -> Unit)? = null
) {
    val base = if (onClick != null) modifier.clickable { onClick() } else modifier
    Card(modifier = base) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(valor, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
            if (detalle != null) {
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
