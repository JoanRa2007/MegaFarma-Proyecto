package pe.edu.upeu.megafarma.app.presentation.reportes

import pe.edu.upeu.megafarma.app.presentation.componentes.Tarjeta
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.edu.upeu.megafarma.app.core.soles
import pe.edu.upeu.megafarma.app.domain.model.GrupoReporte
import pe.edu.upeu.megafarma.app.domain.model.ResumenVentas
import pe.edu.upeu.megafarma.app.presentation.componentes.BarraHorizontal
import pe.edu.upeu.megafarma.app.presentation.componentes.TarjetaDato
import pe.edu.upeu.megafarma.app.presentation.componentes.Titulo
import pe.edu.upeu.megafarma.app.presentation.componentes.Vacio
import pe.edu.upeu.megafarma.app.presentation.tema.ColoresEstado
import kotlin.math.abs
import kotlin.math.roundToLong

/*
 * Vista comun del dashboard y de los reportes por dia, mes y año:
 * indicadores, grafico de barras, top de productos, categorias y medios de pago.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResumenVentasVista(r: ResumenVentas) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("${r.titulo} · del ${r.desde} al ${r.hasta}", color = MaterialTheme.colorScheme.onSurfaceVariant)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaDato("Total vendido", r.totalVendido.soles(), ColoresEstado.ok, Modifier.widthIn(min = 180.dp),
                detalle = variacionTexto(r.variacionPorcentaje))
            TarjetaDato("Ventas", r.cantidadVentas.toString(), MaterialTheme.colorScheme.primary, Modifier.widthIn(min = 150.dp))
            TarjetaDato("Ticket promedio", r.ticketPromedio.soles(), MaterialTheme.colorScheme.secondary, Modifier.widthIn(min = 150.dp))
            TarjetaDato("Unidades vendidas", r.unidadesVendidas.toString(), MaterialTheme.colorScheme.onSurface, Modifier.widthIn(min = 150.dp))
            TarjetaDato("Anuladas", r.ventasAnuladas.toString(), ColoresEstado.peligro, Modifier.widthIn(min = 150.dp),
                detalle = r.totalAnulado.soles())
        }

        Tarjeta(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Titulo(
                    when (r.agrupacion) {
                        "HORA" -> "Ventas por hora"
                        "MES" -> "Ventas por mes"
                        else -> "Ventas por día"
                    }
                )
                Spacer(Modifier.height(12.dp))
                GraficoBarras(r.serie)
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaLista("Top 5 productos", Modifier.widthIn(min = 300.dp, max = 520.dp)) {
                if (r.topProductos.isEmpty()) Vacio("Sin ventas en el período")
                val maximo = r.topProductos.maxOfOrNull { it.unidades }?.takeIf { it > 0 } ?: 1L
                r.topProductos.forEachIndexed { i, p ->
                    BarraHorizontal("${i + 1}. ${p.nombre}", p.unidades.toFloat() / maximo, "${p.unidades} u · ${p.importe.soles()}")
                }
            }
            TarjetaLista("Ventas por categoría", Modifier.widthIn(min = 300.dp, max = 520.dp)) {
                Barras(r.porCategoria)
            }
            TarjetaLista("Medios de pago", Modifier.widthIn(min = 300.dp, max = 520.dp)) {
                Barras(r.porMedioPago)
            }
        }
    }
}

private fun variacionTexto(v: Double?): String = when {
    v == null -> "Sin ventas en el período anterior"
    v >= 0 -> "+${redondear(v)} % vs. período anterior"
    else -> "-${redondear(abs(v))} % vs. período anterior"
}

private fun redondear(v: Double): String {
    val d = (v * 10).roundToLong()
    return "${d / 10}.${abs(d % 10)}"
}

@Composable
private fun TarjetaLista(titulo: String, modifier: Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Tarjeta(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Titulo(titulo)
            Spacer(Modifier.height(4.dp))
            contenido()
        }
    }
}

@Composable
private fun Barras(grupos: List<GrupoReporte>) {
    if (grupos.isEmpty()) {
        Vacio("Sin ventas en el período")
        return
    }
    val maximo = grupos.maxOf { it.total }.takeIf { it > 0 } ?: 1.0
    grupos.forEach { g -> BarraHorizontal(g.etiqueta, (g.total / maximo).toFloat(), g.total.soles()) }
}

/* Grafico de barras verticales. Muestra solo algunas etiquetas para que no se amontonen. */
@Composable
fun GraficoBarras(serie: List<GrupoReporte>, alto: Int = 170) {
    if (serie.isEmpty()) {
        Vacio("Sin datos")
        return
    }
    val maximo = serie.maxOf { it.total }.takeIf { it > 0 } ?: 1.0
    val paso = ((serie.size + 7) / 8).coerceAtLeast(1)
    Text("Máximo: ${maximo.soles()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(Modifier.fillMaxWidth().height(alto.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        serie.forEach { g ->
            val fraccion = (g.total / maximo).toFloat()
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(if (g.total > 0) fraccion.coerceIn(0.03f, 1f) else 0.01f)
                        .background(
                            if (g.total > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                        )
                )
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        serie.forEachIndexed { i, g ->
            Text(
                if (i % paso == 0) g.etiqueta else "",
                Modifier.weight(1f),
                fontSize = 9.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
