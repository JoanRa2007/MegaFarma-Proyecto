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
 * Identidad visual de Megafarma (Material 3), tomada de los sistemas de boticas
 * del mercado: menu lateral casi negro, contenido blanco con grises neutros,
 * verde agua como acento y etiquetas de color suave con borde.
 */
private val Claro = lightColorScheme(
    primary = Color(0xFF009786),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2F4F1),
    onPrimaryContainer = Color(0xFF0B4A43),
    secondary = Color(0xFF4DB5AB),
    onSecondary = Color(0xFF0E2424),
    secondaryContainer = Color(0xFFE6F5F3),
    onSecondaryContainer = Color(0xFF16423D),
    tertiary = Color(0xFFD4932F),
    onTertiary = Color.White,
    background = Color(0xFFF5F6F8),
    onBackground = Color(0xFF2B2F33),
    surface = Color.White,
    onSurface = Color(0xFF2B2F33),
    surfaceVariant = Color(0xFFF9FBFC),
    onSurfaceVariant = Color(0xFF6B7280),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF9FBFC),
    surfaceContainerHigh = Color(0xFFF5F6F8),
    surfaceContainerHighest = Color(0xFFEDEFF2),
    outline = Color(0xFFD5D9DD),
    outlineVariant = Color(0xFFE9ECEF),
    error = Color(0xFFDF5D5D),
    errorContainer = Color(0xFFFDECEC),
    onErrorContainer = Color(0xFF8A2525)
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF4DB5AB),
    onPrimary = Color(0xFF0E2424),
    primaryContainer = Color(0xFF1D4642),
    onPrimaryContainer = Color(0xFFCDEDE9),
    secondary = Color(0xFF4DB5AB),
    onSecondary = Color(0xFF0E2424),
    secondaryContainer = Color(0xFF1F3A37),
    onSecondaryContainer = Color(0xFFCDEDE9),
    tertiary = Color(0xFFE0A856),
    background = Color(0xFF111213),
    onBackground = Color(0xFFE4E6E8),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE4E6E8),
    surfaceVariant = Color(0xFF212427),
    onSurfaceVariant = Color(0xFF9CA3AA),
    surfaceContainerLowest = Color(0xFF151718),
    surfaceContainerLow = Color(0xFF1A1C1E),
    surfaceContainer = Color(0xFF212427),
    surfaceContainerHigh = Color(0xFF26292C),
    surfaceContainerHighest = Color(0xFF2D3134),
    outline = Color(0xFF3A3F44),
    outlineVariant = Color(0xFF2A2E32),
    error = Color(0xFFF08080),
    errorContainer = Color(0xFF4A1C1C),
    onErrorContainer = Color(0xFFFAD4D4)
)

/*
 * Colores de marca que no forman parte del esquema de Material:
 * el menu lateral oscuro y los degradados de las cabeceras.
 */
object ColoresMarca {
    /* Menu lateral (igual en modo claro y oscuro). */
    val menuFondo = Color(0xFF17191A)
    val menuTexto = Color(0xFFA5A9AC)
    val menuTextoSuave = Color(0xFF6B6D71)
    val menuSeleccion = Color(0xFF4DB5AB)
    val menuSeleccionTexto = Color(0xFF0E2424)
    val menuDivisor = Color(0xFF26292B)

    /* Degradado de la pantalla de inicio de sesion y de la bienvenida. */
    val degradado = listOf(Color(0xFF4DB5AB), Color(0xFF009786), Color(0xFF1F6F68))
}

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/* Colores de estado usados en etiquetas e indicadores. */
object ColoresEstado {
    val ok = Color(0xFF5FAE3E)
    val alerta = Color(0xFFD4932F)
    val peligro = Color(0xFFDF5D5D)
    val aviso = Color(0xFFD4A45B)
    val info = Color(0xFF3B82F6)
    val neutro = Color(0xFF8A9099)
}

@Composable
fun MegafarmaTema(oscuro: Boolean, contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (oscuro) Oscuro else Claro, shapes = Formas, content = contenido)
}
