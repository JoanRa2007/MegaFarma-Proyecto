package pe.edu.upeu.megafarma.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.megafarma.app.core.Resultado
import pe.edu.upeu.megafarma.app.core.soles
import pe.edu.upeu.megafarma.app.domain.model.Periodo
import pe.edu.upeu.megafarma.app.domain.model.ResumenAlertas
import pe.edu.upeu.megafarma.app.domain.model.ResumenVentas
import pe.edu.upeu.megafarma.app.domain.repository.InventarioRepository
import pe.edu.upeu.megafarma.app.domain.repository.ReporteRepository
import pe.edu.upeu.megafarma.app.presentation.componentes.Cargando
import pe.edu.upeu.megafarma.app.presentation.componentes.EncabezadoSeccion
import pe.edu.upeu.megafarma.app.presentation.componentes.GraficoDona
import pe.edu.upeu.megafarma.app.presentation.componentes.GraficoLinea
import pe.edu.upeu.megafarma.app.presentation.componentes.GrillaTarjetas
import pe.edu.upeu.megafarma.app.presentation.componentes.Iconos
import pe.edu.upeu.megafarma.app.presentation.componentes.Insignia
import pe.edu.upeu.megafarma.app.presentation.componentes.Leyenda
import pe.edu.upeu.megafarma.app.presentation.componentes.MensajeError
import pe.edu.upeu.megafarma.app.presentation.componentes.Panel
import pe.edu.upeu.megafarma.app.presentation.componentes.Sparkline
import pe.edu.upeu.megafarma.app.presentation.componentes.Titulo
import pe.edu.upeu.megafarma.app.presentation.componentes.Vacio
import pe.edu.upeu.megafarma.app.presentation.componentes.recortarHoras
import pe.edu.upeu.megafarma.app.presentation.navegacion.Destino
import pe.edu.upeu.megafarma.app.presentation.tema.ColoresEstado
import kotlin.math.abs
import kotlin.math.roundToLong

data class DashboardUiState(
    val periodo: Periodo = Periodo.HOY,
    val cargando: Boolean = true,
    val resumen: ResumenVentas? = null,
    val alertas: ResumenAlertas? = null,
    val error: String? = null
)

/*
 * Dashboard (solo ADMIN): vista rapida del dia a dia, al estilo de los sistemas de botica.
 * Arriba las alertas de inventario; luego indicadores con su tendencia, la curva de ventas
 * contra el periodo anterior, los comprobantes emitidos, el top de productos y los medios de pago.
 * Los Reportes, en cambio, son el analisis detallado de un dia, mes o año elegido.
 */
class DashboardViewModel(
    private val reportes: ReporteRepository,
    private val inventario: InventarioRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(DashboardUiState())
    val estado: StateFlow<DashboardUiState> = _estado.asStateFlow()

    fun cargar(periodo: Periodo = _estado.value.periodo) {
        _estado.update { it.copy(periodo = periodo, cargando = true, error = null) }
        viewModelScope.launch {
            // Ventas y alertas se piden a la vez.
            val alertas = async { inventario.resumenAlertas() }
            val resumen = reportes.dashboard(periodo)
            val a = alertas.await()
            _estado.update {
                it.copy(
                    cargando = false,
                    resumen = (resumen as? Resultado.Exito)?.datos ?: it.resumen,
                    alertas = (a as? Resultado.Exito)?.datos ?: it.alertas,
                    error = (resumen as? Resultado.Error)?.mensaje
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(onIrA: (Destino) -> Unit = {}, viewModel: DashboardViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargar() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EncabezadoSeccion("Dashboard", estado.resumen?.titulo ?: "Resumen del negocio") {
            if (estado.cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            IconButton(onClick = { viewModel.cargar() }) {
                Icon(Iconos.Restaurar, contentDescription = "Actualizar", tint = MaterialTheme.colorScheme.primary)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Periodo.HOY, Periodo.SEMANA, Periodo.MES, Periodo.ANIO).forEach { p ->
                FilterChip(selected = estado.periodo == p, onClick = { viewModel.cargar(p) }, label = { Text(p.etiqueta) })
            }
        }

        estado.alertas?.let { FranjaAlertas(it) { onIrA(Destino.ALERTAS) } }

        when {
            estado.cargando && estado.resumen == null -> Cargando()
            estado.error != null && estado.resumen == null -> MensajeError(estado.error!!) { viewModel.cargar() }
            estado.resumen != null -> ContenidoDashboard(estado.resumen!!, onIrA)
        }
    }
}

/* Franja de alertas de inventario (como la barra superior de los sistemas de botica). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FranjaAlertas(a: ResumenAlertas, onVer: () -> Unit) {
    GrillaTarjetas(anchoMinimo = 200.dp, espacio = 10.dp) {
        val m = Modifier.weight(1f).fillMaxRowHeight()
        AvisoAlerta("Stock bajo", "${a.productosBajoMinimo} producto(s) en su mínimo",
            if (a.productosBajoMinimo > 0) ColoresEstado.alerta else ColoresEstado.ok, Iconos.Inventario, m, onVer)
        AvisoAlerta("Vencidos", "${a.lotesVencidos} lote(s) para dar de baja",
            if (a.lotesVencidos > 0) ColoresEstado.peligro else ColoresEstado.ok, Iconos.Advertencia, m, onVer)
        AvisoAlerta("Por vencer", "${a.lotesCriticos} lote(s) en 30 días",
            if (a.lotesCriticos > 0) ColoresEstado.aviso else ColoresEstado.ok, Iconos.Calendario, m, onVer)
    }
}

@Composable
private fun AvisoAlerta(titulo: String, texto: String, color: Color, icono: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.35f), MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.SemiBold, color = color)
            Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("Ver ›", style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContenidoDashboard(r: ResumenVentas, onIrA: (Destino) -> Unit) {
    val totales = r.serie.map { it.total }
    val cantidades = r.serie.map { it.cantidadVentas.toDouble() }

    // ---------- Indicadores con su tendencia ----------
    GrillaTarjetas(anchoMinimo = 210.dp) {
        val m = Modifier.weight(1f).fillMaxRowHeight()
        KpiTendencia("Total vendido", r.totalVendido.soles(), Iconos.Dinero, ColoresEstado.ok, totales, m,
            variacion(r.variacionPorcentaje))
        KpiTendencia("Ventas", r.cantidadVentas.toString(), Iconos.Caja, ColoresEstado.info, cantidades, m,
            "${r.unidadesVendidas} unidades vendidas" to null)
        KpiTendencia("Ticket promedio", r.ticketPromedio.soles(), Iconos.Recibo, Color(0xFF8B5CF6), totales, m,
            "Importe medio por venta" to null)
        ComprobantesEmitidos(r, m) { onIrA(Destino.VENTAS) }
    }

    // ---------- Curva de ventas contra el periodo anterior ----------
    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Titulo(
                when (r.agrupacion) {
                    "HORA" -> "Ventas por hora"
                    "MES" -> "Ventas por mes"
                    else -> "Ventas por día"
                },
                Modifier.weight(1f)
            )
            Text(r.totalVendido.soles(), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Leyenda(MaterialTheme.colorScheme.primary, "Este período")
            if (r.serieAnterior.isNotEmpty()) {
                Leyenda(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    "Período anterior (${r.totalPeriodoAnterior.soles()})", punteada = true)
            }
        }
        Spacer(Modifier.height(12.dp))
        val (serie, anterior) = recortarHoras(r.serie, r.serieAnterior, r.agrupacion)
        GraficoLinea(serie, anterior, alto = 220.dp)
    }

    // ---------- Top de productos y medios de pago ----------
    GrillaTarjetas(anchoMinimo = 320.dp) {
        Panel(Modifier.weight(1f).fillMaxRowHeight()) {
            Titulo("Más vendidos")
            Spacer(Modifier.height(8.dp))
            if (r.topProductos.isEmpty()) Vacio("Sin ventas en el período")
            r.topProductos.forEachIndexed { i, p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(26.dp).clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (i == 0) 1f else 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${i + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                            color = if (i == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                        Text("${p.unidades} unidades", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(p.importe.soles(), fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Panel(Modifier.weight(1f).fillMaxRowHeight()) {
            Titulo("Medios de pago")
            Spacer(Modifier.height(12.dp))
            GraficoDona(r.porMedioPago, centro = "${r.cantidadVentas}\nventas")
        }
    }
}

/* Indicador con mini grafico de tendencia del periodo. */
@Composable
private fun KpiTendencia(
    titulo: String,
    valor: String,
    icono: ImageVector,
    color: Color,
    tendencia: List<Double>,
    modifier: Modifier,
    detalle: Pair<String, Color?>
) {
    Panel(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(valor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Insignia(icono, color, 40)
        }
        Spacer(Modifier.height(8.dp))
        Sparkline(tendencia, color, Modifier.fillMaxWidth().height(36.dp))
        Spacer(Modifier.height(6.dp))
        Text(detalle.first, style = MaterialTheme.typography.bodySmall,
            color = detalle.second ?: MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
    }
}

/* Tarjeta de comprobantes emitidos: boletas y facturas del periodo, y anuladas. */
@Composable
private fun ComprobantesEmitidos(r: ResumenVentas, modifier: Modifier, onVer: () -> Unit) {
    val emitidos = r.boletasEmitidas + r.facturasEmitidas
    Panel(modifier, onClick = onVer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Comprobantes emitidos", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("$emitidos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Insignia(Iconos.Recibo, ColoresEstado.alerta, 40)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small)) {
            if (emitidos == 0) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
            } else {
                if (r.boletasEmitidas > 0) Box(Modifier.weight(r.boletasEmitidas.toFloat()).fillMaxHeight().background(ColoresEstado.info))
                if (r.facturasEmitidas > 0) Box(Modifier.weight(r.facturasEmitidas.toFloat()).fillMaxHeight().background(ColoresEstado.alerta))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Leyenda(ColoresEstado.info, "Boletas ${r.boletasEmitidas}")
            Leyenda(ColoresEstado.alerta, "Facturas ${r.facturasEmitidas}")
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (r.ventasAnuladas > 0) "${r.ventasAnuladas} anulada(s) · ${r.totalAnulado.soles()}" else "Sin anulaciones",
            style = MaterialTheme.typography.bodySmall,
            color = if (r.ventasAnuladas > 0) ColoresEstado.peligro else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun variacion(v: Double?): Pair<String, Color?> = when {
    v == null -> "Sin ventas en el período anterior" to null
    v >= 0 -> "▲ ${uno(v)} % vs. período anterior" to ColoresEstado.ok
    else -> "▼ ${uno(abs(v))} % vs. período anterior" to ColoresEstado.peligro
}

private fun uno(v: Double): String {
    val d = (v * 10).roundToLong()
    return "${d / 10}.${abs(d % 10)}"
}
