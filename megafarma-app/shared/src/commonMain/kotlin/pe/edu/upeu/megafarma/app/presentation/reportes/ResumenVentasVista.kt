package pe.edu.upeu.megafarma.app.presentation.reportes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.edu.upeu.megafarma.app.core.soles
import pe.edu.upeu.megafarma.app.domain.model.GrupoReporte
import pe.edu.upeu.megafarma.app.domain.model.ReporteInventario
import pe.edu.upeu.megafarma.app.domain.model.ResumenVentas
import pe.edu.upeu.megafarma.app.presentation.componentes.BarraHorizontal
import pe.edu.upeu.megafarma.app.presentation.componentes.GraficoBarras
import pe.edu.upeu.megafarma.app.presentation.componentes.GraficoDona
import pe.edu.upeu.megafarma.app.presentation.componentes.GrillaTarjetas
import pe.edu.upeu.megafarma.app.presentation.componentes.Panel
import pe.edu.upeu.megafarma.app.presentation.componentes.Titulo
import pe.edu.upeu.megafarma.app.presentation.componentes.Vacio
import pe.edu.upeu.megafarma.app.presentation.componentes.nombreBonito
import pe.edu.upeu.megafarma.app.presentation.componentes.porcentaje
import pe.edu.upeu.megafarma.app.presentation.componentes.recortarHoras
import pe.edu.upeu.megafarma.app.presentation.tema.ColoresEstado
import kotlin.math.abs
import kotlin.math.roundToLong

/*
 * Cuerpo del reporte (analisis de un dia, mes o año elegido). A diferencia del dashboard,
 * que es una vista rapida con tendencias, aqui se muestran las cifras en tablas:
 * resumen con el periodo anterior, barras con el detalle tramo por tramo, ventas por
 * categoria y por vendedor, productos mas vendidos con su participacion e inventario valorizado.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResumenVentasVista(r: ResumenVentas, inventario: ReporteInventario?) {
    val tramo = when (r.agrupacion) {
        "HORA" -> "Hora"
        "MES" -> "Mes"
        else -> "Día"
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // ---------- Resumen en tabla, con el periodo anterior al lado ----------
        GrillaTarjetas(anchoMinimo = 300.dp) {
            Panel(Modifier.weight(1f).fillMaxRowHeight()) {
                Titulo("Resumen del período")
                Spacer(Modifier.height(8.dp))
                FilaDato("Total vendido", r.totalVendido.soles(), negrita = true)
                FilaDato("Número de ventas", r.cantidadVentas.toString())
                FilaDato("Ticket promedio", r.ticketPromedio.soles())
                FilaDato("Unidades vendidas", r.unidadesVendidas.toString())
                FilaDato("Boletas / facturas", "${r.boletasEmitidas} / ${r.facturasEmitidas}")
                FilaDato("Ventas anuladas", "${r.ventasAnuladas} · ${r.totalAnulado.soles()}",
                    color = if (r.ventasAnuladas > 0) ColoresEstado.peligro else null)
            }
            Panel(Modifier.weight(1f).fillMaxRowHeight()) {
                Titulo("Comparación")
                Spacer(Modifier.height(8.dp))
                FilaDato("Este período", r.totalVendido.soles(), negrita = true)
                FilaDato("Período anterior", r.totalPeriodoAnterior.soles())
                FilaDato("Diferencia", (r.totalVendido - r.totalPeriodoAnterior).soles())
                val v = r.variacionPorcentaje
                FilaDato(
                    "Variación",
                    when {
                        v == null -> "Sin datos anteriores"
                        v >= 0 -> "▲ ${uno(v)} %"
                        else -> "▼ ${uno(abs(v))} %"
                    },
                    color = when {
                        v == null -> null
                        v >= 0 -> ColoresEstado.ok
                        else -> ColoresEstado.peligro
                    }
                )
                val mejor = r.serie.maxByOrNull { it.total }?.takeIf { it.total > 0 }
                FilaDato("Mejor ${tramo.lowercase()}", mejor?.let { "${it.etiqueta} · ${it.total.soles()}" } ?: "—")
            }
        }

        // ---------- Barras con eje ----------
        Panel(Modifier.fillMaxWidth()) {
            Titulo("Ventas por ${tramo.lowercase()}")
            Spacer(Modifier.height(12.dp))
            GraficoBarras(recortarHoras(r.serie, emptyList(), r.agrupacion).first, alto = 200.dp)
        }

        // ---------- Detalle tramo por tramo ----------
        Panel(Modifier.fillMaxWidth()) {
            Titulo("Detalle por ${tramo.lowercase()}")
            Spacer(Modifier.height(8.dp))
            val conVentas = r.serie.filter { it.cantidadVentas > 0 }
            if (conVentas.isEmpty()) {
                Vacio("Sin ventas en el período")
            } else {
                Encabezado(listOf(tramo to 1.2f, "Ventas" to 0.8f, "Total" to 1.2f, "Promedio" to 1.1f, "% del total" to 1f))
                conVentas.forEach { g ->
                    Fila(listOf(
                        g.etiqueta to 1.2f,
                        g.cantidadVentas.toString() to 0.8f,
                        g.total.soles() to 1.2f,
                        (g.total / g.cantidadVentas).soles() to 1.1f,
                        "${porcentaje(g.total, r.totalVendido)} %" to 1f
                    ))
                }
                Fila(listOf("Total" to 1.2f, r.cantidadVentas.toString() to 0.8f, r.totalVendido.soles() to 1.2f,
                    r.ticketPromedio.soles() to 1.1f, "100 %" to 1f), negrita = true)
            }
        }

        // ---------- Categorias (dona) y vendedores (barras) ----------
        GrillaTarjetas(anchoMinimo = 320.dp) {
            Panel(Modifier.weight(1f).fillMaxRowHeight()) {
                Titulo("Ventas por categoría")
                Spacer(Modifier.height(12.dp))
                GraficoDona(r.porCategoria, tamano = 140.dp)
            }
            Panel(Modifier.weight(1f).fillMaxRowHeight()) {
                Titulo("Ventas por vendedor")
                Spacer(Modifier.height(8.dp))
                Barras(r.porVendedor, conCantidad = true)
                Spacer(Modifier.height(12.dp))
                Titulo("Medios de pago")
                Spacer(Modifier.height(8.dp))
                Barras(r.porMedioPago)
            }
        }

        // ---------- Productos mas vendidos ----------
        Panel(Modifier.fillMaxWidth()) {
            Titulo("Productos más vendidos")
            Spacer(Modifier.height(8.dp))
            if (r.topProductos.isEmpty()) {
                Vacio("Sin ventas en el período")
            } else {
                Encabezado(listOf("#" to 0.4f, "Producto" to 2.6f, "Unidades" to 1f, "Importe" to 1.1f, "% ventas" to 0.9f))
                r.topProductos.forEachIndexed { i, p ->
                    Fila(listOf(
                        "${i + 1}" to 0.4f, p.nombre to 2.6f, p.unidades.toString() to 1f,
                        p.importe.soles() to 1.1f, "${porcentaje(p.importe, r.totalVendido)} %" to 0.9f
                    ))
                }
            }
        }

        // ---------- Inventario valorizado ----------
        inventario?.let { inv ->
            Panel(Modifier.fillMaxWidth()) {
                Titulo("Inventario valorizado (hoy)")
                Spacer(Modifier.height(8.dp))
                FilaDato("Valor a precio de venta", inv.valorTotal.soles(), negrita = true)
                FilaDato("Unidades disponibles", inv.unidadesTotales.toString())
                FilaDato("Productos con stock", inv.productosConStock.toString())
            }
        }
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String, negrita: Boolean = false, color: Color? = null) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(etiqueta, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, fontWeight = if (negrita) FontWeight.Bold else FontWeight.SemiBold,
                color = color ?: MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun Encabezado(columnas: List<Pair<String, Float>>) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        columnas.forEachIndexed { i, (t, peso) ->
            Text(t.uppercase(), Modifier.weight(peso), style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                textAlign = if (i == 0) TextAlign.Start else TextAlign.End, softWrap = false, overflow = TextOverflow.Clip)
        }
    }
}

@Composable
private fun Fila(celdas: List<Pair<String, Float>>, negrita: Boolean = false) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
            celdas.forEachIndexed { i, (t, peso) ->
                Text(t, Modifier.weight(peso), style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, fontWeight = if (negrita) FontWeight.Bold else FontWeight.Normal,
                    textAlign = if (i == 0) TextAlign.Start else TextAlign.End)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun Barras(grupos: List<GrupoReporte>, conCantidad: Boolean = false) {
    if (grupos.isEmpty()) {
        Text("Sin ventas en el período", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val maximo = grupos.maxOf { it.total }.takeIf { it > 0 } ?: 1.0
    grupos.forEach { g ->
        val nombre = nombreBonito(g.etiqueta).substringBefore(" ")
        BarraHorizontal(
            if (conCantidad) "$nombre (${g.cantidadVentas})" else nombreBonito(g.etiqueta),
            (g.total / maximo).toFloat(),
            g.total.soles()
        )
    }
}

private fun uno(v: Double): String {
    val d = (v * 10).roundToLong()
    return "${d / 10}.${abs(d % 10)}"
}
