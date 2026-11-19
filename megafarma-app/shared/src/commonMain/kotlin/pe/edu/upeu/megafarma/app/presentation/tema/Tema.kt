package pe.edu.upeu.megafarma.app.presentation.tema

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
 * Identidad visual de Megafarma (Material 3): verde azulado clinico,
 * superficies blancas sobre fondo gris claro, bordes redondeados y
 * modo oscuro con la misma paleta.
 */
private val Claro = lightColorScheme(
    primary = Color(0xFF0E7C66),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3F4EA),
    onPrimaryContainer = Color(0xFF00382C),
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF3EC),
    onSecondaryContainer = Color(0xFF0B5A4A),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    background = Color(0xFFF4F6F9),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF2F6),
    onSurfaceVariant = Color(0xFF5B6678),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF3F5F8),
    surfaceContainerHigh = Color(0xFFF8FAFC),
    surfaceContainerHighest = Color(0xFFE9EEF4),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE5E9F0),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF34D3B0),
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF0B4E41),
    onPrimaryContainer = Color(0xFFC6F5E8),
    secondary = Color(0xFF7AA7FF),
    onSecondary = Color(0xFF0B1E4A),
    secondaryContainer = Color(0xFF123F36),
    onSecondaryContainer = Color(0xFFBFF0E2),
    tertiary = Color(0xFFFBBF24),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF111A2B),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1B2638),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF0E1625),
    surfaceContainerLow = Color(0xFF131D30),
    surfaceContainer = Color(0xFF172236),
    surfaceContainerHigh = Color(0xFF1A263B),
    surfaceContainerHighest = Color(0xFF223049),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF243247),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF4C1515),
    onErrorContainer = Color(0xFFFECACA)
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/* Colores de estado usados en etiquetas e indicadores. */
object ColoresEstado {
    val ok = Color(0xFF059669)
    val alerta = Color(0xFFEA580C)
    val peligro = Color(0xFFDC2626)
    val aviso = Color(0xFFCA8A04)
    val info = Color(0xFF2563EB)
    val neutro = Color(0xFF64748B)
}

@Composable
fun MegafarmaTema(oscuro: Boolean, contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (oscuro) Oscuro else Claro, shapes = Formas, content = contenido)
}
