package pe.edu.upeu.megafarma.app.presentation.navegacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.launch
import pe.edu.upeu.megafarma.app.domain.model.Permiso
import pe.edu.upeu.megafarma.app.domain.model.Producto
import pe.edu.upeu.megafarma.app.domain.model.Sesion
import pe.edu.upeu.megafarma.app.presentation.alertas.AlertasScreen
import pe.edu.upeu.megafarma.app.presentation.componentes.Avatar
import pe.edu.upeu.megafarma.app.presentation.componentes.Iconos
import pe.edu.upeu.megafarma.app.presentation.inicio.InicioScreen
import pe.edu.upeu.megafarma.app.presentation.productos.ProductoDetalleScreen
import pe.edu.upeu.megafarma.app.presentation.productos.ProductosScreen
import pe.edu.upeu.megafarma.app.presentation.reportes.ReportesScreen
import pe.edu.upeu.megafarma.app.presentation.tema.ColoresMarca
import pe.edu.upeu.megafarma.app.presentation.ventas.NuevaVentaScreen
import pe.edu.upeu.megafarma.app.presentation.ventas.VentasScreen

/* Secciones del menu lateral. */
enum class Grupo(val titulo: String) {
    OPERACION("Operación"),
    INVENTARIO("Inventario"),
    GESTION("Gestión")
}

/* Pantallas de la app. Cada una exige un permiso; si el rol no lo tiene, no aparece. */
enum class Destino(
    val titulo: String,
    val descripcion: String,
    val permiso: String?,
    val grupo: Grupo,
    val icono: () -> ImageVector
) {
    INICIO("Inicio", "Resumen del día y accesos rápidos", null, Grupo.OPERACION, { Iconos.Inicio }),
    NUEVA_VENTA("Caja", "Registrar ventas y emitir comprobantes", Permiso.VENTAS_REGISTRAR, Grupo.OPERACION, { Iconos.Caja }),
    VENTAS("Ventas", "Historial, comprobantes y anulaciones", Permiso.VENTAS_LEER, Grupo.OPERACION, { Iconos.Recibo }),
    PRODUCTOS("Productos y stock", "Catálogo, lotes y entradas", null, Grupo.INVENTARIO, { Iconos.Inventario }),
    ALERTAS("Alertas", "Stock mínimo y vencimientos por nivel", Permiso.INVENTARIO_LEER, Grupo.INVENTARIO, { Iconos.Alerta }),
    DASHBOARD("Dashboard", "Indicadores de ventas en tiempo real", Permiso.REPORTES_VER, Grupo.GESTION, { Iconos.Dashboard }),
    REPORTES("Reportes", "Ventas por día, mes y año", Permiso.REPORTES_VER, Grupo.GESTION, { Iconos.Reportes }),
    USUARIOS("Usuarios", "Cuentas, roles y accesos", Permiso.USUARIOS_ADMINISTRAR, Grupo.GESTION, { Iconos.Usuarios }),
    BITACORA("Seguridad", "Bitácora de auditoría", Permiso.USUARIOS_ADMINISTRAR, Grupo.GESTION, { Iconos.Escudo })
}

/*
 * Pantalla principal con navegacion responsiva:
 *  - Pantalla ancha (web, tablet): menu lateral fijo con secciones e iconos.
 *  - Pantalla angosta (celular): menu desplegable.
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
            MenuLateral(sesion, destinos, destino, oscuro, { irA(it); cerrar() }, onCambiarTema, onCerrarSesion)
        }
        val contenido: @Composable (onAbrirMenu: (() -> Unit)?) -> Unit = { onAbrirMenu ->
            Contenido(sesion, destino, productoAbierto, onAbrirMenu, irA, { productoAbierto = it }, { productoAbierto = null }, ancho)
        }

        if (ancho) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(Modifier.width(272.dp), drawerContainerColor = ColoresMarca.menuFondo) { menu {} }
                }
            ) { contenido(null) }
        } else {
            val estadoMenu = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            ModalNavigationDrawer(
                drawerState = estadoMenu,
                drawerContent = { ModalDrawerSheet(Modifier.width(290.dp), drawerContainerColor = ColoresMarca.menuFondo) { menu { scope.launch { estadoMenu.close() } } } }
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
    Column(Modifier.fillMaxHeight()) {
        // Marca
        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(ColoresMarca.menuSeleccion),
                contentAlignment = Alignment.Center
            ) {
                Icon(Iconos.Farmacia, null, tint = ColoresMarca.menuSeleccionTexto, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Megafarma", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Punto de venta", style = MaterialTheme.typography.labelMedium, color = ColoresMarca.menuTextoSuave)
            }
        }
        HorizontalDivider(color = ColoresMarca.menuDivisor)

        // Navegacion por secciones
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
            Grupo.entries.forEach { grupo ->
                val delGrupo = destinos.filter { it.grupo == grupo }
                if (delGrupo.isNotEmpty()) {
                    Text(
                        grupo.titulo.uppercase(),
                        Modifier.padding(start = 28.dp, top = 14.dp, bottom = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ColoresMarca.menuTextoSuave
                    )
                    delGrupo.forEach { d ->
                        NavigationDrawerItem(
                            icon = { Icon(d.icono(), contentDescription = null, modifier = Modifier.size(20.dp)) },
                            label = { Text(d.titulo, fontWeight = if (d == actual) FontWeight.SemiBold else FontWeight.Normal) },
                            selected = d == actual,
                            onClick = { onElegir(d) },
                            shape = RoundedCornerShape(10.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = ColoresMarca.menuSeleccion,
                                selectedIconColor = ColoresMarca.menuSeleccionTexto,
                                selectedTextColor = ColoresMarca.menuSeleccionTexto,
                                unselectedContainerColor = Color.Transparent,
                                unselectedIconColor = ColoresMarca.menuTexto,
                                unselectedTextColor = ColoresMarca.menuTexto
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp).height(44.dp)
                        )
                    }
                }
            }
        }

        // Usuario y acciones
        HorizontalDivider(color = ColoresMarca.menuDivisor)
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(sesion.nombreCompleto)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(sesion.nombreCompleto, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${sesion.username} · ${sesion.rol}", style = MaterialTheme.typography.labelSmall,
                    color = ColoresMarca.menuTextoSuave, maxLines = 1)
            }
            IconButton(onClick = onCambiarTema) {
                Icon(if (oscuro) Iconos.Sol else Iconos.Luna, contentDescription = if (oscuro) "Modo claro" else "Modo oscuro",
                    tint = ColoresMarca.menuTexto, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onCerrarSesion) {
                Icon(Iconos.Salir, contentDescription = "Cerrar sesión", tint = Color(0xFFFF8A94), modifier = Modifier.size(20.dp))
            }
        }
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
    onCerrarProducto: () -> Unit,
    ancho: Boolean
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(productoAbierto?.nombre ?: destino.titulo, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            if (ancho) {
                                Text(if (productoAbierto != null) "Detalle del producto y stock por lote" else destino.descripcion,
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    navigationIcon = {
                        when {
                            productoAbierto != null -> IconButton(onClick = onCerrarProducto) { Icon(Iconos.Atras, "Atrás") }
                            onAbrirMenu != null -> IconButton(onClick = onAbrirMenu) { Icon(Iconos.Menu, "Menú") }
                        }
                    },
                    actions = {
                        if (ancho) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(end = 16.dp)
                            ) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Iconos.Candado, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Sesión segura · ${sesion.rol}", style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    ) { relleno ->
        Box(Modifier.fillMaxSize().padding(relleno)) {
            when {
                productoAbierto != null -> ProductoDetalleScreen(productoAbierto, sesion, onVolver = onCerrarProducto)
                destino == Destino.INICIO -> InicioScreen(sesion, onIrA)
                destino == Destino.PRODUCTOS -> ProductosScreen(sesion, onAbrirProducto)
                destino == Destino.NUEVA_VENTA -> NuevaVentaScreen()
                destino == Destino.VENTAS -> VentasScreen(sesion)
                destino == Destino.ALERTAS -> AlertasScreen()
                destino == Destino.REPORTES -> ReportesScreen()
                destino == Destino.DASHBOARD -> pe.edu.upeu.megafarma.app.presentation.dashboard.DashboardScreen()
                destino == Destino.USUARIOS -> pe.edu.upeu.megafarma.app.presentation.usuarios.UsuariosScreen()
                destino == Destino.BITACORA -> pe.edu.upeu.megafarma.app.presentation.bitacora.BitacoraScreen()
            }
        }
    }
}
