package pe.edu.upeu.megafarma.app.presentation.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.edu.upeu.megafarma.app.core.soles
import pe.edu.upeu.megafarma.app.domain.model.GrupoReporte
import kotlin.math.ceil
import kotlin.math.roundToLong

/*
 * Graficos dibujados a mano con Canvas (sin librerias): linea con area, barras,
 * dona y mini linea (sparkline). Los ejes muestran pocas etiquetas, espaciadas segun
 * el ancho disponible, para que nunca se amontonen ni se corten.
 */

/* Colores para series y categorias (dona, leyendas). */
val PALETA_GRAFICOS = listOf(
    Color(0xFF2F9E8F), Color(0xFF3B82F6), Color(0xFFD4932F), Color(0xFF8B5CF6),
    Color(0xFFDF5D5D), Color(0xFF5FAE3E), Color(0xFF0EA5E9), Color(0xFF8A9099)
)

private val ANCHO_EJE_Y = 52.dp
private val ANCHO_ETIQUETA_X = 44.dp

/* 1234.5 -> "S/ 1.2 mil"; 350 -> "S/ 350": para los ejes, donde no hay espacio. */
fun Double.solesCorto(): String = when {
    this >= 1_000_000 -> "S/ ${decimal1(this / 1_000_000)} M"
    this >= 1_000 -> "S/ ${decimal1(this / 1_000)} mil"
    else -> "S/ ${this.roundToLong()}"
}

private fun decimal1(v: Double): String {
    val d = (v * 10).roundToLong()
    return if (d % 10 == 0L) "${d / 10}" else "${d / 10}.${d % 10}"
}

/* "08:00" -> "8h"; las demas etiquetas quedan igual. */
private fun etiquetaCorta(texto: String): String =
    if (texto.length == 5 && texto[2] == ':') "${texto.take(2).trimStart('0').ifEmpty { "0" }}h" else texto

/*
 * En el grafico por hora se ocultan las horas sin movimiento al inicio y al final
 * (madrugada), pero siempre se muestra de 8h a 20h. Aplica el mismo recorte a la serie anterior.
 */
fun recortarHoras(serie: List<GrupoReporte>, anterior: List<GrupoReporte>, agrupacion: String): Pair<List<GrupoReporte>, List<GrupoReporte>> {
    if (agrupacion != "HORA" || serie.size != 24) return serie to anterior
    val conVentas = serie.indices.filter { i -> serie[i].total > 0 || (anterior.getOrNull(i)?.total ?: 0.0) > 0 }
    val desde = minOf(8, conVentas.minOrNull() ?: 8)
    val hasta = maxOf(20, conVentas.maxOrNull() ?: 20)
    return serie.subList(desde, hasta + 1) to (if (anterior.size == 24) anterior.subList(desde, hasta + 1) else anterior)
}

/* Etiquetas del eje Y: maximo "redondo", su mitad y cero. */
@Composable
private fun EjeY(maximo: Double, alto: Dp) {
    Column(Modifier.width(ANCHO_EJE_Y).height(alto), verticalArrangement = Arrangement.SpaceBetween) {
        listOf(maximo, maximo / 2, 0.0).forEach {
            Text(it.solesCorto(), fontSize = 10.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/*
 * Eje X: cada etiqueta se centra en su punto (fraccion 0..1 del ancho). Se elige un paso
 * para que quepan sin taparse: si hay 31 dias y poco ancho, se muestra 1 de cada 4.
 */
@Composable
private fun EjeX(etiquetas: List<String>, centrado: Boolean) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(start = ANCHO_EJE_Y).height(16.dp)) {
        val n = etiquetas.size
        if (n == 0) return@BoxWithConstraints
        val caben = (maxWidth / (ANCHO_ETIQUETA_X + 6.dp)).toInt().coerceAtLeast(2)
        val paso = ceil(n / caben.toDouble()).toInt().coerceAtLeast(1)
        val limite = (maxWidth - ANCHO_ETIQUETA_X).coerceAtLeast(0.dp)
        etiquetas.forEachIndexed { i, texto ->
            if (i % paso != 0) return@forEachIndexed
            val fraccion = when {
                centrado -> (i + 0.5f) / n
                n == 1 -> 0.5f
                else -> i / (n - 1f)
            }
            val x = (maxWidth * fraccion - ANCHO_ETIQUETA_X / 2).coerceIn(0.dp, limite)
            Text(
                etiquetaCorta(texto),
                Modifier.width(ANCHO_ETIQUETA_X).offset(x = x),
                fontSize = 10.sp,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* Maximo del eje redondeado hacia arriba (10, 20, 50, 100, 200, 500...). */
private fun maximoRedondo(valor: Double): Double {
    if (valor <= 0) return 10.0
    var base = 1.0
    while (base * 10 <= valor) base *= 10
    return listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * base }.first { it >= valor }
}

/*
 * Grafico de linea con area sombreada. Si llega la serie anterior, se dibuja punteada
 * en gris para comparar ("este mes" contra "el mes pasado").
 */
@Composable
fun GraficoLinea(
    serie: List<GrupoReporte>,
    anterior: List<GrupoReporte> = emptyList(),
    alto: Dp = 200.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    if (serie.isEmpty()) {
        Vacio("Sin datos")
        return
    }
    val maximo = maximoRedondo(maxOf(serie.maxOf { it.total }, anterior.maxOfOrNull { it.total } ?: 0.0))
    val rejilla = MaterialTheme.colorScheme.outlineVariant
    val gris = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val fondo = MaterialTheme.colorScheme.surface

    Column {
        Row {
            EjeY(maximo, alto)
            Canvas(Modifier.weight(1f).height(alto)) {
                val w = size.width
                val h = size.height
                for (k in 0..2) {
                    val y = h * k / 2f
                    drawLine(rejilla, Offset(0f, y), Offset(w, y), strokeWidth = 1f,
                        pathEffect = if (k == 2) null else PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                }
                fun punto(i: Int, valor: Double, n: Int): Offset {
                    val x = if (n <= 1) w / 2 else w * i / (n - 1f)
                    return Offset(x, h - (valor / maximo).toFloat().coerceIn(0f, 1f) * h)
                }
                if (anterior.isNotEmpty()) {
                    val previo = Path()
                    anterior.forEachIndexed { i, g ->
                        val p = punto(i, g.total, anterior.size)
                        if (i == 0) previo.moveTo(p.x, p.y) else previo.lineTo(p.x, p.y)
                    }
                    drawPath(previo, gris, style = Stroke(width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
                }
                val linea = Path()
                val area = Path()
                serie.forEachIndexed { i, g ->
                    val p = punto(i, g.total, serie.size)
                    if (i == 0) {
                        linea.moveTo(p.x, p.y)
                        area.moveTo(p.x, h)
                        area.lineTo(p.x, p.y)
                    } else {
                        linea.lineTo(p.x, p.y)
                        area.lineTo(p.x, p.y)
                    }
                }
                area.lineTo(punto(serie.size - 1, 0.0, serie.size).x, h)
                area.close()
                drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.02f))))
                drawPath(linea, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                if (serie.size <= 31) {
                    serie.forEachIndexed { i, g ->
                        val p = punto(i, g.total, serie.size)
                        drawCircle(fondo, 4.dp.toPx(), p)
                        drawCircle(color, 2.8.dp.toPx(), p)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        EjeX(serie.map { it.etiqueta }, centrado = false)
    }
}

/* Barras verticales con eje Y; la barra mas alta se resalta. */
@Composable
fun GraficoBarras(serie: List<GrupoReporte>, alto: Dp = 200.dp, color: Color = MaterialTheme.colorScheme.primary) {
    if (serie.isEmpty()) {
        Vacio("Sin datos")
        return
    }
    val maximo = maximoRedondo(serie.maxOf { it.total })
    val mayor = serie.indices.maxByOrNull { serie[it].total } ?: -1
    val rejilla = MaterialTheme.colorScheme.outlineVariant
    val vacio = MaterialTheme.colorScheme.surfaceVariant
    Column {
        Row {
            EjeY(maximo, alto)
            Canvas(Modifier.weight(1f).height(alto)) {
                val w = size.width
                val h = size.height
                for (k in 0..2) {
                    val y = h * k / 2f
                    drawLine(rejilla, Offset(0f, y), Offset(w, y), strokeWidth = 1f,
                        pathEffect = if (k == 2) null else PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                }
                val ancho = w / serie.size
                val barra = (ancho * 0.7f).coerceAtMost(36.dp.toPx())
                serie.forEachIndexed { i, g ->
                    val altoBarra = if (g.total > 0) ((g.total / maximo).toFloat().coerceIn(0.02f, 1f) * h) else 2f
                    val c = when {
                        g.total <= 0 -> vacio
                        i == mayor -> color
                        else -> color.copy(alpha = 0.6f)
                    }
                    drawRoundRect(
                        c,
                        topLeft = Offset(ancho * i + (ancho - barra) / 2, h - altoBarra),
                        size = Size(barra, altoBarra),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        EjeX(serie.map { it.etiqueta }, centrado = true)
    }
}

/* Mini linea sin ejes para las tarjetas de indicadores. */
@Composable
fun Sparkline(valores: List<Double>, color: Color, modifier: Modifier = Modifier) {
    if (valores.size < 2) return
    val maximo = valores.max().takeIf { it > 0 } ?: 1.0
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val linea = Path()
        val area = Path()
        valores.forEachIndexed { i, v ->
            val x = w * i / (valores.size - 1f)
            val y = h - (v / maximo).toFloat() * h * 0.9f
            if (i == 0) {
                linea.moveTo(x, y); area.moveTo(x, h); area.lineTo(x, y)
            } else {
                linea.lineTo(x, y); area.lineTo(x, y)
            }
        }
        area.lineTo(w, h)
        area.close()
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), Color.Transparent)))
        drawPath(linea, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

/* Dona con leyenda: participacion de cada grupo en el total (medios de pago, categorias). */
@Composable
fun GraficoDona(grupos: List<GrupoReporte>, centro: String? = null, tamano: Dp = 150.dp) {
    if (grupos.isEmpty() || grupos.sumOf { it.total } <= 0) {
        Vacio("Sin ventas en el período")
        return
    }
    val total = grupos.sumOf { it.total }
    val colores = grupos.indices.map { PALETA_GRAFICOS[it % PALETA_GRAFICOS.size] }
    val pista = MaterialTheme.colorScheme.surfaceVariant
    // Dona arriba y leyenda debajo. Sin BoxWithConstraints: la dona va dentro de tarjetas
    // de igual alto (GrillaTarjetas), que miden su contenido por adelantado, y ese
    // componente no admite esa medicion (la app se cerraba al abrir el Dashboard).
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(tamano), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val grosor = size.minDimension * 0.18f
                val diametro = size.minDimension - grosor
                val esquina = Offset((size.width - diametro) / 2, (size.height - diametro) / 2)
                drawArc(pista, 0f, 360f, false, esquina, Size(diametro, diametro), style = Stroke(grosor))
                var inicio = -90f
                grupos.forEachIndexed { i, g ->
                    val barrido = (g.total / total * 360).toFloat()
                    drawArc(colores[i], inicio, (barrido - 1.5f).coerceAtLeast(0.5f), false, esquina,
                        Size(diametro, diametro), style = Stroke(grosor))
                    inicio += barrido
                }
            }
            if (centro != null) {
                Text(centro, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            grupos.forEachIndexed { i, g ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(colores[i], CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(nombreBonito(g.etiqueta), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${porcentaje(g.total, total)} %", style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold)
                    Text(g.total.soles(), Modifier.width(86.dp), style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/* Leyenda simple con un punto de color (por ejemplo "Este periodo" y "Periodo anterior"). */
@Composable
fun Leyenda(color: Color, texto: String, punteada: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.width(if (punteada) 14.dp else 10.dp).height(if (punteada) 3.dp else 10.dp)
                .background(color, if (punteada) RoundedCornerShape(2.dp) else CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/* "EFECTIVO" -> "Efectivo"; los nombres normales quedan igual. */
fun nombreBonito(texto: String): String =
    if (texto.isNotEmpty() && texto == texto.uppercase()) texto.lowercase().replaceFirstChar { it.uppercase() } else texto

/* Porcentaje con un decimal: 12.5 */
fun porcentaje(parte: Double, total: Double): String = if (total <= 0) "0" else decimal1(parte / total * 100)
