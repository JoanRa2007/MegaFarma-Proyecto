package pe.edu.upeu.megafarma.app.presentation.navegacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pe.edu.upeu.megafarma.app.domain.model.Permiso
import pe.edu.upeu.megafarma.app.domain.model.Producto
import pe.edu.upeu.megafarma.app.domain.model.Sesion
import pe.edu.upeu.megafarma.app.presentation.alertas.AlertasScreen
import pe.edu.upeu.megafarma.app.presentation.inicio.InicioScreen
import pe.edu.upeu.megafarma.app.presentation.productos.ProductoDetalleScreen
import pe.edu.upeu.megafarma.app.presentation.productos.ProductosScreen
import pe.edu.upeu.megafarma.app.presentation.reportes.ReportesScreen
import pe.edu.upeu.megafarma.app.presentation.ventas.NuevaVentaScreen
import pe.edu.upeu.megafarma.app.presentation.ventas.VentasScreen

/* Secciones del menu. Cada una exige un permiso; si el rol no lo tiene, no aparece. */
enum class Destino(val titulo: String, val permiso: String?) {
    INICIO("Inicio", null),
    PRODUCTOS("Productos y stock", null),
    NUEVA_VENTA("Nueva venta", Permiso.VENTAS_REGISTRAR),
    VENTAS("Ventas", Permiso.VENTAS_LEER),
    ALERTAS("Alertas", Permiso.INVENTARIO_LEER),
    REPORTES("Reportes", Permiso.REPORTES_VER)
}

/*
 * Pantalla principal con navegacion responsiva:
 *  - Pantalla ancha (web, tablet): menu lateral fijo.
 *  - Pantalla angosta (celular): menu que se abre con el boton "Menú".
 */
@Composable
fun PantallaPrincipal(
    sesion: Sesion,
    oscuro: Boolean,
    onCambiarTema: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val destinos = Destino.entries.filter { it.permiso == null || sesion.puede(it.permiso) }
    var destino by remember(sesion.username) { mutableStateOf(Destino.INICIO) }
    var productoAbierto by remember(sesion.username) { mutableStateOf<Producto?>(null) }

    val irA: (Destino) -> Unit = {
        destino = it
        productoAbierto = null
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val ancho = maxWidth >= 840.dp

        val menu: @Composable (cerrar: () -> Unit) -> Unit = { cerrar ->
            MenuLateral(
                sesion = sesion,
                destinos = destinos,
                actual = destino,
                oscuro = oscuro,
                onElegir = { irA(it); cerrar() },
                onCambiarTema = onCambiarTema,
                onCerrarSesion = onCerrarSesion
            )
        }

        val contenido: @Composable (onAbrirMenu: (() -> Unit)?) -> Unit = { onAbrirMenu ->
            Contenido(
                sesion = sesion,
                destino = destino,
                productoAbierto = productoAbierto,
                onAbrirMenu = onAbrirMenu,
                onIrA = irA,
                onAbrirProducto = { productoAbierto = it },
                onCerrarProducto = { productoAbierto = null }
            )
        }

        if (ancho) {
            PermanentNavigationDrawer(
                drawerContent = { PermanentDrawerSheet(Modifier.width(270.dp)) { menu {} } }
            ) { contenido(null) }
        } else {
            val estadoMenu = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            ModalNavigationDrawer(
                drawerState = estadoMenu,
                drawerContent = { ModalDrawerSheet { menu { scope.launch { estadoMenu.close() } } } }
            ) { contenido { scope.launch { estadoMenu.open() } } }
        }
    }
}

@Composable
private fun MenuLateral(
    sesion: Sesion,
    destinos: List<Destino>,
    actual: Destino,
    oscuro: Boolean,
    onElegir: (Destino) -> Unit,
    onCambiarTema: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Megafarma", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(sesion.nombreCompleto, style = MaterialTheme.typography.bodyLarge)
            Text("${sesion.username} · ${sesion.rol}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        destinos.forEach { d ->
            NavigationDrawerItem(
                label = { Text(d.titulo) },
                selected = d == actual,
                onClick = { onElegir(d) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        NavigationDrawerItem(
            label = { Text(if (oscuro) "Modo claro" else "Modo oscuro") },
            selected = false,
            onClick = onCambiarTema,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Cerrar sesión") },
            selected = false,
            onClick = onCerrarSesion,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Contenido(
    sesion: Sesion,
    destino: Destino,
    productoAbierto: Producto?,
    onAbrirMenu: (() -> Unit)?,
    onIrA: (Destino) -> Unit,
    onAbrirProducto: (Producto) -> Unit,
    onCerrarProducto: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(productoAbierto?.nombre ?: destino.titulo, maxLines = 1) },
                navigationIcon = {
                    // Texto blanco sobre la barra verde (TextButton usa el color primario por defecto).
                    val colores = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                    when {
                        productoAbierto != null -> TextButton(onClick = onCerrarProducto, colors = colores) { Text("< Atrás") }
                        onAbrirMenu != null -> TextButton(onClick = onAbrirMenu, colors = colores) { Text("Menú") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { relleno ->
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(relleno)
        ) {
            when {
                productoAbierto != null -> ProductoDetalleScreen(productoAbierto, sesion, onVolver = onCerrarProducto)
                destino == Destino.INICIO -> InicioScreen(sesion, onIrA)
                destino == Destino.PRODUCTOS -> ProductosScreen(sesion, onAbrirProducto)
                destino == Destino.NUEVA_VENTA -> NuevaVentaScreen()
                destino == Destino.VENTAS -> VentasScreen(sesion)
                destino == Destino.ALERTAS -> AlertasScreen()
                destino == Destino.REPORTES -> ReportesScreen()
            }
        }
    }
}
