package pe.edu.upeu.megafarma.app.presentation.bitacora

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.megafarma.app.core.Resultado
import pe.edu.upeu.megafarma.app.core.fechaHora
import pe.edu.upeu.megafarma.app.core.rangoMostrado
import pe.edu.upeu.megafarma.app.data.remote.TAMANIO_BITACORA
import pe.edu.upeu.megafarma.app.domain.model.RegistroAuditoria
import pe.edu.upeu.megafarma.app.domain.repository.UsuarioRepository
import pe.edu.upeu.megafarma.app.presentation.componentes.Cargando
import pe.edu.upeu.megafarma.app.presentation.componentes.Celda
import pe.edu.upeu.megafarma.app.presentation.componentes.Columna
import pe.edu.upeu.megafarma.app.presentation.componentes.EncabezadoSeccion
import pe.edu.upeu.megafarma.app.presentation.componentes.EncabezadoTabla
import pe.edu.upeu.megafarma.app.presentation.componentes.Etiqueta
import pe.edu.upeu.megafarma.app.presentation.componentes.Iconos
import pe.edu.upeu.megafarma.app.presentation.componentes.FilaTabla
import pe.edu.upeu.megafarma.app.presentation.componentes.MensajeError
import pe.edu.upeu.megafarma.app.presentation.componentes.Paginador
import pe.edu.upeu.megafarma.app.presentation.componentes.Panel
import pe.edu.upeu.megafarma.app.presentation.componentes.Tabla
import pe.edu.upeu.megafarma.app.presentation.componentes.Vacio
import pe.edu.upeu.megafarma.app.presentation.tema.ColoresEstado

/* Filtros rapidos de la bitacora: etiqueta -> accion ('' = todas). */
private val FILTROS = listOf(
    "Todo" to "",
    "Inicios de sesión" to "LOGIN_OK",
    "Intentos fallidos" to "LOGIN_FALLIDO",
    "Cuentas bloqueadas" to "CUENTA_BLOQUEADA",
    "Accesos denegados" to "ACCESO_DENEGADO",
    "Fuerza bruta" to "LIMITE_INTENTOS",
    "Ventas" to "VENTA_REGISTRADA",
    "Anulaciones" to "VENTA_ANULADA"
)

data class BitacoraUiState(
    val usuario: String = "",
    val accion: String = "",
    val cargando: Boolean = true,
    val registros: List<RegistroAuditoria> = emptyList(),
    val pagina: Int = 0,
    val totalPaginas: Int = 0,
    val totalElementos: Long = 0,
    val error: String? = null
)

/*
 * Bitacora de seguridad (HU-45): quien hizo que, cuando y desde que IP. Solo ADMIN.
 * El buscador filtra solo mientras se escribe (sin boton) y se muestran 10 eventos por pagina.
 */
class BitacoraViewModel(private val repositorio: UsuarioRepository) : ViewModel() {

    private val _estado = MutableStateFlow(BitacoraUiState())
    val estado: StateFlow<BitacoraUiState> = _estado.asStateFlow()

    /* Consulta en curso: si llega otra (se siguio escribiendo), la anterior se cancela. */
    private var consulta: Job? = null

    fun cargar(pagina: Int = 0, espera: Long = 0) {
        _estado.update { it.copy(cargando = true, error = null) }
        consulta?.cancel()
        consulta = viewModelScope.launch {
            if (espera > 0) delay(espera)
            val e = _estado.value
            when (val r = repositorio.bitacora(e.usuario.trim(), e.accion, pagina)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(cargando = false, registros = r.datos.registros, pagina = r.datos.pagina,
                        totalPaginas = r.datos.totalPaginas, totalElementos = r.datos.totalElementos)
                }
                is Resultado.Error -> _estado.update { it.copy(cargando = false, error = r.mensaje) }
            }
        }
    }

    /* Filtra 400 ms despues de la ultima tecla, para no consultar por cada letra. */
    fun cambiarUsuario(valor: String) {
        val limpio = valor.filter { it.isLetterOrDigit() || it in "._-" }.take(50)
        _estado.update { it.copy(usuario = limpio) }
        cargar(0, espera = 400)
    }

    fun filtrar(accion: String) {
        _estado.update { it.copy(accion = accion) }
        cargar()
    }
}

/* Color de cada accion: verde si fue exitosa, rojo si fue un intento rechazado. */
private fun colorAccion(r: RegistroAuditoria) = when {
    !r.exito -> ColoresEstado.peligro
    r.accion.startsWith("VENTA") -> ColoresEstado.info
    r.accion.startsWith("LOGIN") || r.accion == "LOGOUT" -> ColoresEstado.ok
    else -> ColoresEstado.neutro
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BitacoraScreen(viewModel: BitacoraViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargar() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Sin titulo propio: la barra superior ya dice "Seguridad · Bitacora de auditoria".
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = estado.usuario,
                onValueChange = viewModel::cambiarUsuario,
                placeholder = { Text("Buscar usuario") },
                leadingIcon = { Icon(Iconos.Buscar, null, Modifier.size(20.dp)) },
                trailingIcon = if (estado.usuario.isNotEmpty()) ({
                    TextButton(onClick = { viewModel.cambiarUsuario("") }) { Text("Limpiar") }
                }) else null,
                singleLine = true,
                modifier = Modifier.weight(1f, fill = false).widthIn(min = 200.dp, max = 320.dp)
            )
            Spacer(Modifier.weight(1f))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Iconos.Escudo, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${estado.totalElementos} eventos", style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FILTROS.forEach { (etiqueta, accion) ->
                FilterChip(selected = estado.accion == accion, onClick = { viewModel.filtrar(accion) }, label = { Text(etiqueta) })
            }
        }

        Box(Modifier.weight(1f)) {
            when {
                estado.cargando && estado.registros.isEmpty() && estado.error == null -> Cargando()
                estado.error != null -> MensajeError(estado.error!!) { viewModel.cargar() }
                estado.registros.isEmpty() -> Vacio("No hay eventos con ese filtro")
                else -> BoxWithConstraints {
                    if (maxWidth >= 760.dp) {
                        Tabla {
                            EncabezadoTabla(listOf(Columna("Fecha", 1.3f), Columna("Usuario", 1f), Columna("Acción", 1.5f),
                                Columna("Detalle", 2.6f), Columna("IP", 1f)))
                            LazyColumn {
                                items(estado.registros, key = { it.id }) { r ->
                                    FilaTabla {
                                        Celda(r.fecha.fechaHora(), 1.3f, secundario = true)
                                        Celda(r.usuario, 1f, negrita = true)
                                        Box(Modifier.weight(1.5f)) { Etiqueta(r.accion, colorAccion(r)) }
                                        Celda(r.detalle ?: "", 2.6f)
                                        Celda(r.ip ?: "-", 1f, secundario = true)
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(estado.registros, key = { it.id }) { r ->
                                Panel(Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Etiqueta(r.accion, colorAccion(r))
                                        Spacer(Modifier.weight(1f))
                                        Text(r.fecha.fechaHora(), style = MaterialTheme.typography.labelSmall)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(r.usuario, fontWeight = FontWeight.SemiBold)
                                    Text("${r.detalle ?: ""} · IP ${r.ip ?: "-"}", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        Paginador(
            estado.pagina, estado.totalPaginas,
            { viewModel.cargar(estado.pagina - 1) }, { viewModel.cargar(estado.pagina + 1) },
            detalle = rangoMostrado(estado.pagina, TAMANIO_BITACORA, estado.registros.size, estado.totalElementos)
        )
    }
}
