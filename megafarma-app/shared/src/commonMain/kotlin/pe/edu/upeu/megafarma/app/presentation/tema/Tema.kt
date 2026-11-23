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
 * Identidad visual de Megafarma (Material 3), inspirada en los sistemas de
 * boticas del mercado: verde azulado como color principal, menu lateral
 * oscuro, superficies blancas y rojo solo para acciones destructivas.
 */
private val Claro = lightColorScheme(
    primary = Color(0xFF009684),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCEFEA),
    onPrimaryContainer = Color(0xFF00382F),
    secondary = Color(0xFF2A5A54),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEFEC),
    onSecondaryContainer = Color(0xFF1B3F3A),
    tertiary = Color(0xFFE8892B),
    onTertiary = Color.White,
    background = Color(0xFFF3F6F6),
    onBackground = Color(0xFF13201F),
    surface = Color.White,
    onSurface = Color(0xFF13201F),
    surfaceVariant = Color(0xFFEAF1F0),
    onSurfaceVariant = Color(0xFF55676A),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF1F5F4),
    surfaceContainerHigh = Color(0xFFF7FAF9),
    surfaceContainerHighest = Color(0xFFE5EDEC),
    outline = Color(0xFFC9D6D4),
    outlineVariant = Color(0xFFE2EAE9),
    error = Color(0xFFD22A3C),
    errorContainer = Color(0xFFFCE4E6),
    onErrorContainer = Color(0xFF7A1420)
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF3CC9B5),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF0A4F46),
    onPrimaryContainer = Color(0xFFC4F2EA),
    secondary = Color(0xFF8CC7BF),
    onSecondary = Color(0xFF0E2A27),
    secondaryContainer = Color(0xFF1F403C),
    onSecondaryContainer = Color(0xFFC9ECE6),
    tertiary = Color(0xFFF5A75A),
    background = Color(0xFF0D1817),
    onBackground = Color(0xFFE1ECEA),
    surface = Color(0xFF13211F),
    onSurface = Color(0xFFE1ECEA),
    surfaceVariant = Color(0xFF1C2D2B),
    onSurfaceVariant = Color(0xFF9AB0AD),
    surfaceContainerLowest = Color(0xFF0F1C1A),
    surfaceContainerLow = Color(0xFF142321),
    surfaceContainer = Color(0xFF182826),
    surfaceContainerHigh = Color(0xFF1C2E2C),
    surfaceContainerHighest = Color(0xFF243836),
    outline = Color(0xFF34504C),
    outlineVariant = Color(0xFF24393A),
    error = Color(0xFFF2737E),
    errorContainer = Color(0xFF4E1219),
    onErrorContainer = Color(0xFFFCD0D4)
)

/*
 * Colores de marca que no forman parte del esquema de Material:
 * el menu lateral oscuro y los degradados de las cabeceras.
 */
object ColoresMarca {
    /* Menu lateral (igual en modo claro y oscuro). */
    val menuFondo = Color(0xFF1F4A45)
    val menuTexto = Color(0xFFD7E8E5)
    val menuTextoSuave = Color(0xFF8FB3AE)
    val menuSeleccion = Color(0xFF009684)
    val menuDivisor = Color(0xFF2E5D57)

    /* Degradado de la pantalla de inicio de sesion y de la bienvenida. */
    val degradado = listOf(Color(0xFF009684), Color(0xFF00786A), Color(0xFF1F4A45))
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
    val ok = Color(0xFF1E9E6A)
    val alerta = Color(0xFFE8892B)
    val peligro = Color(0xFFD22A3C)
    val aviso = Color(0xFFD9A21B)
    val info = Color(0xFF2F7FC1)
    val neutro = Color(0xFF64748B)
}

@Composable
fun MegafarmaTema(oscuro: Boolean, contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (oscuro) Oscuro else Claro, shapes = Formas, content = contenido)
}
