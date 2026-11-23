package pe.edu.upeu.megafarma.app.presentation.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* Componentes con estilo de panel profesional: KPI, tablas, avatar y encabezados de seccion. */

/* Tarjeta con borde suave: base de todas las tarjetas del panel. */
@Composable
fun Panel(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, contenido: @Composable ColumnScope.() -> Unit) {
    val base = modifier
        .clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
    Column((if (onClick != null) base.clickable { onClick() } else base).padding(16.dp), content = contenido)
}

/* Indicador (KPI) con icono en una insignia de color. */
@Composable
fun Kpi(
    titulo: String,
    valor: String,
    icono: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    detalle: String? = null,
    colorDetalle: Color? = null,
    onClick: (() -> Unit)? = null
) {
    Panel(modifier, onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Text(valor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
            }
            Insignia(icono, color)
        }
        if (detalle != null) {
            Spacer(Modifier.height(8.dp))
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = colorDetalle ?: MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun Insignia(icono: ImageVector, color: Color, tamano: Int = 44) {
    Box(
        Modifier.size(tamano.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size((tamano / 2).dp))
    }
}

/* Circulo con las iniciales del usuario. */
@Composable
fun Avatar(nombre: String, tamano: Int = 38) {
    val iniciales = nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(
        Modifier.size(tamano.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(iniciales.ifEmpty { "?" }, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge)
    }
}

/*
 * Titulo de seccion con subtitulo y acciones. En pantallas anchas las acciones van a la
 * derecha; en el celular van debajo del titulo para que el texto no quede aplastado.
 */
@Composable
fun EncabezadoSeccion(titulo: String, subtitulo: String? = null, acciones: @Composable RowScope.() -> Unit = {}) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val angosto = maxWidth < ANCHO_CELULAR
        val textos = @Composable { m: Modifier ->
            Column(m) {
                Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (subtitulo != null) {
                    Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (angosto) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                textos(Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically, content = acciones)
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                textos(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = acciones)
            }
        }
    }
}

/* Ancho a partir del cual se considera pantalla de celular. */
val ANCHO_CELULAR = 600.dp

/*
 * Grilla de tarjetas que llena el ancho: tantas por fila como quepan con el ancho minimo
 * (en el celular, dos por fila). Cada tarjeta debe usar Modifier.weight(1f).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GrillaTarjetas(
    anchoMinimo: Dp = 160.dp,
    espacio: Dp = 12.dp,
    contenido: @Composable FlowRowScope.() -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val porFila = ((maxWidth + espacio) / (anchoMinimo + espacio)).toInt().coerceAtLeast(1)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(espacio),
            verticalArrangement = Arrangement.spacedBy(espacio),
            maxItemsInEachRow = porFila,
            content = contenido
        )
    }
}

/* ---------- Tabla de datos (pantallas anchas) ---------- */

data class Columna(val titulo: String, val peso: Float)

@Composable
fun EncabezadoTabla(columnas: List<Columna>) {
    Row(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        columnas.forEach { c ->
            Text(c.titulo.uppercase(), Modifier.weight(c.peso), style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
        }
    }
}

@Composable
fun FilaTabla(onClick: (() -> Unit)? = null, contenido: @Composable RowScope.() -> Unit) {
    val base = Modifier.fillMaxWidth()
    Column {
        Row(
            (if (onClick != null) base.clickable { onClick() } else base).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = contenido
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun RowScope.Celda(texto: String, peso: Float, negrita: Boolean = false, secundario: Boolean = false) {
    Text(
        texto,
        Modifier.weight(peso).padding(end = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (negrita) FontWeight.SemiBold else FontWeight.Normal,
        color = if (secundario) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/* Contenedor con borde para la tabla completa. */
@Composable
fun Tabla(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
        content = contenido
    )
}

/* Tarjeta blanca con borde fino (reemplaza a Card en todas las pantallas). */
@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    colors: CardColors? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = colors ?: CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (colors == null) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        content = contenido
    )
}
