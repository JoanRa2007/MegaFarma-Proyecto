package pe.edu.upeu.megafarma.app.presentation.comprobante

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.edu.upeu.megafarma.app.core.fechaHora
import pe.edu.upeu.megafarma.app.core.soles
import pe.edu.upeu.megafarma.app.domain.model.Comprobante
import pe.edu.upeu.megafarma.app.presentation.componentes.Etiqueta
import pe.edu.upeu.megafarma.app.presentation.tema.ColoresEstado

/*
 * Boleta o factura electronica mostrada como ticket. Modo demostracion:
 * tiene la estructura de SUNAT (serie, correlativo, IGV, total en letras,
 * hash y datos del QR), pero no se envia a SUNAT.
 */
@Composable
fun ComprobanteDialog(
    comprobante: Comprobante?,
    cargando: Boolean,
    error: String?,
    xml: String?,
    onVerXml: () -> Unit,
    onCerrarXml: () -> Unit,
    onCerrar: () -> Unit,
    textoBotonCerrar: String = "Cerrar"
) {
    if (xml != null) {
        AlertDialog(
            onDismissRequest = onCerrarXml,
            title = { Text("XML UBL 2.1") },
            text = {
                Box(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
                    Text(xml, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            },
            confirmButton = { Button(onClick = onCerrarXml) { Text("Volver") } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = null,
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).widthIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                when {
                    cargando -> LinearProgressIndicator(Modifier.fillMaxWidth())
                    error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                    comprobante != null -> Ticket(comprobante)
                }
            }
        },
        confirmButton = { Button(onClick = onCerrar) { Text(textoBotonCerrar) } },
        dismissButton = if (comprobante != null) ({ TextButton(onClick = onVerXml) { Text("Ver XML") } }) else null
    )
}

@Composable
private fun Ticket(c: Comprobante) {
    val centro = Modifier.fillMaxWidth()
    Text(c.emisorRazonSocial, centro, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
    Text("RUC ${c.emisorRuc}", centro, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
    Text(c.emisorDireccion, centro, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(4.dp))
    Text(
        if (c.tipo == "FACTURA") "FACTURA ELECTRÓNICA" else "BOLETA DE VENTA ELECTRÓNICA",
        centro, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold
    )
    Text(c.numero, centro, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Row(centro, horizontalArrangement = Arrangement.Center) {
        if (c.estado == "ANULADO") Etiqueta("ANULADO", ColoresEstado.peligro)
        else Etiqueta("Modo demostración · no enviado a SUNAT", ColoresEstado.neutro)
    }
    HorizontalDivider(Modifier.padding(vertical = 4.dp))
    Text("Fecha: ${c.fechaEmision.fechaHora()} · Venta ${c.ventaNumero}", style = MaterialTheme.typography.bodySmall)
    Text(
        "Cliente: ${c.clienteNombre ?: "CLIENTES VARIOS"}" +
            (if (!c.clienteNumDoc.isNullOrBlank() && c.clienteNumDoc != "-") " · ${c.clienteTipoDoc} ${c.clienteNumDoc}" else ""),
        style = MaterialTheme.typography.bodySmall
    )
    HorizontalDivider(Modifier.padding(vertical = 4.dp))
    Row {
        Text("Cant.", Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall)
        Text("Descripción", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
        Text("Importe", style = MaterialTheme.typography.labelSmall)
    }
    c.items.forEach { i ->
        Row {
            Text("${i.cantidad}", Modifier.width(40.dp), style = MaterialTheme.typography.bodySmall)
            Column(Modifier.weight(1f)) {
                Text(i.descripcion, style = MaterialTheme.typography.bodySmall)
                Text("${i.precioUnitario.soles()} c/u", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(i.importe.soles(), style = MaterialTheme.typography.bodySmall)
        }
    }
    HorizontalDivider(Modifier.padding(vertical = 4.dp))
    Linea("Op. gravada", c.opGravada.soles())
    Linea("IGV 18 %", c.igv.soles())
    Linea("TOTAL", c.total.soles(), negrita = true)
    Text("SON: ${c.totalEnLetras}", style = MaterialTheme.typography.labelSmall)
    HorizontalDivider(Modifier.padding(vertical = 4.dp))
    c.pagos.forEach { p ->
        Linea("Pago ${p.medio}" + (p.referencia?.let { " ($it)" } ?: ""), p.monto.soles())
    }
    c.montoRecibido?.let { Linea("Efectivo recibido", it.soles()) }
    c.vuelto?.let { if (it > 0) Linea("Vuelto", it.soles(), negrita = true) }
    HorizontalDivider(Modifier.padding(vertical = 4.dp))
    Text("Hash: ${c.hash.take(28)}…", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
    Text("Datos del QR: ${c.textoQr}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
}

@Composable
private fun Linea(etiqueta: String, valor: String, negrita: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, Modifier.weight(1f), fontWeight = if (negrita) FontWeight.Bold else FontWeight.Normal)
        Text(valor, fontWeight = if (negrita) FontWeight.Bold else FontWeight.Normal)
    }
}
