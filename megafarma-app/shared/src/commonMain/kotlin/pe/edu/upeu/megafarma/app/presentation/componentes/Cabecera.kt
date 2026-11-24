package pe.edu.upeu.megafarma.app.presentation.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.edu.upeu.megafarma.app.presentation.tema.ColoresMarca

/*
 * Cabecera con degradado para Dashboard y Reportes (como la bienvenida de Inicio).
 * Reemplaza al titulo repetido: la barra superior ya dice en que seccion se esta,
 * asi que aqui se muestra el periodo, las cifras clave y los botones de periodo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CabeceraResumen(
    etiqueta: String,
    titulo: String,
    detalle: String?,
    cargando: Boolean,
    opciones: @Composable FlowRowScope.() -> Unit,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
            .background(Brush.horizontalGradient(ColoresMarca.degradado))
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(etiqueta.uppercase(), style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(titulo, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                if (detalle != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(detalle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                }
            }
            if (cargando) {
                CircularProgressIndicator(Modifier.padding(top = 4.dp).size(20.dp), color = Color.White, strokeWidth = 2.dp)
            }
            acciones()
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = opciones)
    }
}

/* Boton tipo pastilla sobre el degradado: blanco si esta elegido, translucido si no. */
@Composable
fun PastillaPeriodo(texto: String, elegido: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape)
            .background(if (elegido) Color.White else Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (elegido) FontWeight.Bold else FontWeight.Medium,
            color = if (elegido) ColoresMarca.degradado[2] else Color.White
        )
    }
}
