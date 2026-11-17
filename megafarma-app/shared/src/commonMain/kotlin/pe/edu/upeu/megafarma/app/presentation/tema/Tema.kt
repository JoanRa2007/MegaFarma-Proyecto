package pe.edu.upeu.megafarma.app.presentation.tema

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/* Colores Material 3 de Megafarma en modo claro y oscuro. */
private val Claro = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF0B3D0F),
    secondary = Color(0xFF00796B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF00332C),
    tertiary = Color(0xFFE65100),
    background = Color(0xFFF4F8F5),
    surface = Color.White,
    surfaceVariant = Color(0xFFE6EEE8),
    error = Color(0xFFC62828)
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF0B3D0F),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color(0xFF00332C),
    secondaryContainer = Color(0xFF004D40),
    onSecondaryContainer = Color(0xFFB2DFDB),
    tertiary = Color(0xFFFFB74D),
    background = Color(0xFF101412),
    surface = Color(0xFF1A201C),
    surfaceVariant = Color(0xFF26302A),
    error = Color(0xFFEF9A9A)
)

/* Colores de estado usados en etiquetas (stock bajo, vencido, etc.). */
object ColoresEstado {
    val ok = Color(0xFF2E7D32)
    val alerta = Color(0xFFE65100)
    val peligro = Color(0xFFC62828)
    val neutro = Color(0xFF546E7A)
}

@Composable
fun MegafarmaTema(oscuro: Boolean, contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (oscuro) Oscuro else Claro, content = contenido)
}
