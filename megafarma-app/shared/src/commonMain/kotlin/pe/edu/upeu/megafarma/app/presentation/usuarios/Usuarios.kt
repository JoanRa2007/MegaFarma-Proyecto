package pe.edu.upeu.megafarma.app.presentation.usuarios

import pe.edu.upeu.megafarma.app.presentation.componentes.Tarjeta
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import pe.edu.upeu.megafarma.app.core.TAMANIO_TABLA
import pe.edu.upeu.megafarma.app.core.rangoMostrado
import pe.edu.upeu.megafarma.app.domain.model.DatosUsuario
import pe.edu.upeu.megafarma.app.domain.model.Usuario
import pe.edu.upeu.megafarma.app.domain.repository.UsuarioRepository
import pe.edu.upeu.megafarma.app.presentation.componentes.ANCHO_CELULAR
import pe.edu.upeu.megafarma.app.presentation.componentes.Aviso
import pe.edu.upeu.megafarma.app.presentation.componentes.Avatar
import pe.edu.upeu.megafarma.app.presentation.componentes.BotonAccion
import pe.edu.upeu.megafarma.app.presentation.componentes.Celda
import pe.edu.upeu.megafarma.app.presentation.componentes.Columna
import pe.edu.upeu.megafarma.app.presentation.componentes.EncabezadoTabla
import pe.edu.upeu.megafarma.app.presentation.componentes.FilaTabla
import pe.edu.upeu.megafarma.app.presentation.componentes.Iconos
import pe.edu.upeu.megafarma.app.presentation.componentes.Paginador
import pe.edu.upeu.megafarma.app.presentation.componentes.Tabla
import pe.edu.upeu.megafarma.app.presentation.componentes.Vacio
import pe.edu.upeu.megafarma.app.presentation.componentes.Campo
import pe.edu.upeu.megafarma.app.presentation.componentes.Cargando
import pe.edu.upeu.megafarma.app.presentation.componentes.Etiqueta
import pe.edu.upeu.megafarma.app.presentation.componentes.MensajeError
import pe.edu.upeu.megafarma.app.presentation.tema.ColoresEstado

/* Roles disponibles y lo que puede hacer cada uno. */
val ROLES = listOf(
    "ADMIN" to "Todo: anula ventas, ve reportes y administra usuarios",
    "VENDEDOR" to "Ventas, clientes y consulta de stock",
    "ALMACENERO" to "Catálogo, entradas por lote, ajustes y bajas"
)

data class UsuariosUiState(
    val cargando: Boolean = true,
    val usuarios: List<Usuario> = emptyList(),
    val error: String? = null,
    val aviso: String? = null,
    val avisoEsError: Boolean = false,
    /* null = cerrado; Usuario con id 0 = nuevo */
    val editando: Usuario? = null,
    val guardando: Boolean = false,
    val errorFormulario: String? = null,
    /* Texto del buscador: nombre, usuario o rol. */
    val texto: String = "",
    /* Cuenta que espera confirmacion para desactivarse. */
    val porDesactivar: Usuario? = null,
    /* Cuenta que espera confirmacion para restablecer su contrasena. */
    val porRestablecer: Usuario? = null,
    /* Contrasena temporal recien generada: se muestra una sola vez al admin. */
    val temporal: Pair<Usuario, String>? = null,
    /* Paginacion de la tabla, con busqueda y 10 filas por pagina en servidor. */
    val pagina: Int = 0,
    val totalPaginas: Int = 0,
    val totalElementos: Long = 0
)

/* Administracion de usuarios (HU-22 a HU-24), solo ADMIN. */
class UsuariosViewModel(private val repositorio: UsuarioRepository) : ViewModel() {

    private val _estado = MutableStateFlow(UsuariosUiState())
    val estado: StateFlow<UsuariosUiState> = _estado.asStateFlow()

    private var busqueda: Job? = null

    fun cargar(pagina: Int = _estado.value.pagina, cancelarBusqueda: Boolean = true) {
        if (cancelarBusqueda) busqueda?.cancel()
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val actual = _estado.value
            when (val r = repositorio.listar(actual.texto, pagina, TAMANIO_TABLA)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(
                        cargando = false,
                        usuarios = r.datos.elementos,
                        pagina = r.datos.pagina,
                        totalPaginas = r.datos.totalPaginas,
                        totalElementos = r.datos.totalElementos
                    )
                }
                is Resultado.Error -> _estado.update { it.copy(cargando = false, error = r.mensaje) }
            }
        }
    }

    fun paginaSiguiente() {
        val e = _estado.value
        if (e.pagina + 1 < e.totalPaginas) cargar(e.pagina + 1)
    }

    fun paginaAnterior() {
        val e = _estado.value
        if (e.pagina > 0) cargar(e.pagina - 1)
    }

    fun nuevo() = _estado.update { it.copy(editando = Usuario(0, "", "", "VENDEDOR", true, false), errorFormulario = null) }

    fun editar(usuario: Usuario) = _estado.update { it.copy(editando = usuario, errorFormulario = null) }

    fun cerrarFormulario() = _estado.update { it.copy(editando = null) }

    fun cerrarAviso() = _estado.update { it.copy(aviso = null) }

    /* Busca en el servidor 350 ms despues de la ultima tecla. */
    fun buscar(texto: String) {
        _estado.update { it.copy(texto = texto, pagina = 0) }
        busqueda?.cancel()
        busqueda = viewModelScope.launch {
            delay(350)
            cargar(0, cancelarBusqueda = false)
        }
    }

    fun pedirDesactivar(usuario: Usuario) = _estado.update { it.copy(porDesactivar = usuario) }

    fun cancelarDesactivar() = _estado.update { it.copy(porDesactivar = null) }

    fun confirmarDesactivar() {
        val usuario = _estado.value.porDesactivar ?: return
        _estado.update { it.copy(porDesactivar = null) }
        cambiarEstado(usuario)
    }

    fun guardar(datos: DatosUsuario) {
        val actual = _estado.value.editando ?: return
        val esNuevo = actual.id == 0L
        val error = when {
            datos.nombres.isBlank() -> "Ingrese los nombres"
            datos.apellidos.isBlank() -> "Ingrese los apellidos"
            datos.dni.isNotBlank() && !(datos.dni.length == 8 && datos.dni.all { it.isDigit() }) -> "El DNI debe tener 8 dígitos"
            datos.telefono.isNotBlank() && !(datos.telefono.length == 9 && datos.telefono.startsWith("9") && datos.telefono.all { it.isDigit() }) ->
                "El teléfono debe tener 9 dígitos y empezar con 9"
            datos.correo.isNotBlank() && !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(datos.correo.trim()) -> "El correo no tiene un formato válido"
            datos.username.trim().length < 3 -> "El usuario debe tener al menos 3 caracteres"
            else -> null
        }
        if (error != null) {
            _estado.update { it.copy(errorFormulario = error) }
            return
        }
        _estado.update { it.copy(guardando = true, errorFormulario = null) }
        viewModelScope.launch {
            when (val r = repositorio.guardar(if (esNuevo) null else actual.id, datos)) {
                is Resultado.Exito -> {
                    val clave = r.datos.passwordTemporal
                    _estado.update {
                        it.copy(
                            guardando = false, editando = null, avisoEsError = false,
                            aviso = if (esNuevo) "Usuario ${r.datos.username} creado" else "Usuario ${r.datos.username} actualizado",
                            temporal = if (clave != null) r.datos to clave else null
                        )
                    }
                    cargar()
                }
                is Resultado.Error -> _estado.update { it.copy(guardando = false, errorFormulario = r.mensaje) }
            }
        }
    }

    fun pedirRestablecer(usuario: Usuario) = _estado.update { it.copy(porRestablecer = usuario) }

    fun cancelarRestablecer() = _estado.update { it.copy(porRestablecer = null) }

    /* Recuperar contrasena: genera una temporal y el trabajador la cambia al entrar. */
    fun confirmarRestablecer() {
        val usuario = _estado.value.porRestablecer ?: return
        _estado.update { it.copy(porRestablecer = null) }
        viewModelScope.launch {
            when (val r = repositorio.restablecerPassword(usuario.id)) {
                is Resultado.Exito -> {
                    val clave = r.datos.passwordTemporal
                    _estado.update { it.copy(temporal = if (clave != null) r.datos to clave else null) }
                    cargar()
                }
                is Resultado.Error -> _estado.update { it.copy(aviso = r.mensaje, avisoEsError = true) }
            }
        }
    }

    fun cerrarTemporal() = _estado.update { it.copy(temporal = null) }

    fun cambiarEstado(usuario: Usuario) {
        viewModelScope.launch {
            val activar = !usuario.activo || usuario.bloqueado
            when (val r = repositorio.cambiarEstado(usuario.id, activar)) {
                is Resultado.Exito -> {
                    _estado.update {
                        it.copy(avisoEsError = false, aviso = if (activar) "Cuenta ${usuario.username} activada" else "Cuenta ${usuario.username} desactivada")
                    }
                    cargar()
                }
                is Resultado.Error -> _estado.update { it.copy(aviso = r.mensaje, avisoEsError = true) }
            }
        }
    }
}

@Composable
fun UsuariosScreen(viewModel: UsuariosViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargar() }

    val q = estado.texto.trim()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = estado.texto,
                onValueChange = viewModel::buscar,
                placeholder = { Text("Buscar por nombre, usuario, DNI o rol") },
                leadingIcon = { Icon(Iconos.Buscar, null, Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = viewModel::nuevo, contentPadding = PaddingValues(horizontal = 14.dp)) {
                Icon(Iconos.Agregar, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Nuevo")
            }
        }
        estado.aviso?.let { Aviso(it, estado.avisoEsError, viewModel::cerrarAviso) }

        when {
            estado.cargando && estado.usuarios.isEmpty() -> Cargando()
            estado.error != null -> MensajeError(estado.error!!) { viewModel.cargar() }
            estado.usuarios.isEmpty() -> Vacio(if (q.isEmpty()) "No hay usuarios registrados" else "Ningún usuario coincide con \"${estado.texto}\"")
            else -> {
                BoxWithConstraints(Modifier.weight(1f)) {
                    val ancho = maxWidth >= ANCHO_CELULAR
                    Tabla {
                        EncabezadoTabla(
                            if (ancho) listOf(Columna("Nombre", 2.4f), Columna("Usuario", 1.2f), Columna("DNI", 1f), Columna("Rol", 1.2f),
                                Columna("Estado", 1.3f), Columna("Acciones", 1.2f))
                            else listOf(Columna("Trabajador", 3f), Columna("", 1.2f))
                        )
                        LazyColumn {
                            items(estado.usuarios, key = { it.id }) { u ->
                                FilaUsuario(u, ancho, onEditar = { viewModel.editar(u) }, onRestablecer = { viewModel.pedirRestablecer(u) },
                                    onDesactivar = { viewModel.pedirDesactivar(u) }, onActivar = { viewModel.cambiarEstado(u) })
                            }
                        }
                    }
                }
                Paginador(
                    pagina = estado.pagina,
                    totalPaginas = estado.totalPaginas,
                    onAnterior = viewModel::paginaAnterior,
                    onSiguiente = viewModel::paginaSiguiente,
                    detalle = rangoMostrado(estado.pagina, TAMANIO_TABLA, estado.usuarios.size, estado.totalElementos)
                )
            }
        }
    }

    estado.editando?.let { u ->
        UsuarioFormDialog(u, estado.guardando, estado.errorFormulario, viewModel::guardar, viewModel::cerrarFormulario)
    }

    estado.porRestablecer?.let { u ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarRestablecer,
            title = { Text("¿Restablecer la contraseña de ${u.nombreCompleto}?") },
            text = { Text("Se generará una contraseña temporal. Su contraseña actual dejará de funcionar y deberá crear una nueva al ingresar.") },
            confirmButton = { Button(onClick = viewModel::confirmarRestablecer) { Text("Restablecer") } },
            dismissButton = { TextButton(onClick = viewModel::cancelarRestablecer) { Text("Cancelar") } }
        )
    }

    estado.temporal?.let { (u, clave) ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Contraseña temporal de ${u.username}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Entréguela al trabajador. Solo se muestra esta vez; al ingresar, el sistema le pedirá crear su propia contraseña.")
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                        SelectionContainer {
                            Text(
                                clave,
                                Modifier.fillMaxWidth().padding(14.dp),
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = viewModel::cerrarTemporal) { Text("Listo, ya la anoté") } }
        )
    }

    estado.porDesactivar?.let { u ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarDesactivar,
            title = { Text("¿Desactivar la cuenta de ${u.nombreCompleto}?") },
            text = { Text("No podrá iniciar sesión hasta que la reactive. Sus ventas y registros se conservan.") },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmarDesactivar,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Desactivar") }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelarDesactivar) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun EstadoUsuario(u: Usuario) = when {
    u.bloqueado -> Etiqueta("Bloqueado", ColoresEstado.peligro, Iconos.Candado)
    u.activo -> Etiqueta("Activo", ColoresEstado.ok)
    else -> Etiqueta("Desactivado", ColoresEstado.neutro)
}

@Composable
private fun FilaUsuario(
    u: Usuario,
    ancho: Boolean,
    onEditar: () -> Unit,
    onRestablecer: () -> Unit,
    onDesactivar: () -> Unit,
    onActivar: () -> Unit
) {
    FilaTabla {
        if (ancho) {
            Row(Modifier.weight(2.4f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(u.nombreCompleto, 32)
                Spacer(Modifier.width(10.dp))
                Text(u.nombreCompleto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Celda(u.username, 1.2f, secundario = true)
            Celda(u.dni ?: "—", 1f, secundario = true)
            Box(Modifier.weight(1.2f)) { Etiqueta(u.rol, colorRol(u.rol)) }
            Box(Modifier.weight(1.3f)) { EstadoUsuario(u) }
        } else {
            Column(Modifier.weight(3f).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(u.nombreCompleto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(u.username, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Etiqueta(u.rol, colorRol(u.rol))
                    EstadoUsuario(u)
                }
            }
        }
        Row(Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically) {
            BotonAccion(Iconos.Editar, "Editar", ColoresEstado.alerta, onEditar)
            BotonAccion(Iconos.Llave, "Restablecer contraseña", ColoresEstado.info, onRestablecer)
            if (u.activo && !u.bloqueado) BotonAccion(Iconos.Eliminar, "Desactivar", ColoresEstado.peligro, onDesactivar)
            else BotonAccion(Iconos.Restaurar, if (u.bloqueado) "Desbloquear" else "Activar", ColoresEstado.ok, onActivar)
        }
    }
}

private fun colorRol(rol: String) = when (rol) {
    "ADMIN" -> ColoresEstado.info
    "ALMACENERO" -> ColoresEstado.alerta
    else -> ColoresEstado.ok
}

@Composable
private fun UsuarioFormDialog(
    usuario: Usuario,
    guardando: Boolean,
    error: String?,
    onGuardar: (DatosUsuario) -> Unit,
    onCerrar: () -> Unit
) {
    val esNuevo = usuario.id == 0L
    // Cuentas antiguas sin nombres/apellidos: se separa el nombre completo como sugerencia.
    val partes = usuario.nombreCompleto.trim().split(" ", limit = 2)
    var nombres by remember(usuario.id) { mutableStateOf(usuario.nombres ?: partes.getOrElse(0) { "" }) }
    var apellidos by remember(usuario.id) { mutableStateOf(usuario.apellidos ?: partes.getOrElse(1) { "" }) }
    var dni by remember(usuario.id) { mutableStateOf(usuario.dni ?: "") }
    var telefono by remember(usuario.id) { mutableStateOf(usuario.telefono ?: "") }
    var correo by remember(usuario.id) { mutableStateOf(usuario.correo ?: "") }
    var username by remember(usuario.id) { mutableStateOf(usuario.username) }
    var rol by remember(usuario.id) { mutableStateOf(usuario.rol) }
    var abierto by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!guardando) onCerrar() },
        title = { Text(if (esNuevo) "Nuevo trabajador" else "Editar ${usuario.username}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Datos personales", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Campo(nombres, { nombres = it }, "Nombres *")
                Campo(apellidos, { apellidos = it }, "Apellidos *")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Campo(dni, { if (it.length <= 8) dni = it.filter { c -> c.isDigit() } }, "DNI", Modifier.weight(1f), numerico = true)
                    Campo(telefono, { if (it.length <= 9) telefono = it.filter { c -> c.isDigit() } }, "Teléfono", Modifier.weight(1f), numerico = true)
                }
                Campo(correo, { correo = it.trim() }, "Correo")

                Spacer(Modifier.height(4.dp))
                Text("Acceso al sistema", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Campo(username, { username = it.trim().lowercase() }, "Usuario *")
                // Lista desplegable de roles: se toca el campo y se elige.
                Box {
                    OutlinedTextField(
                        value = rol,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rol *") },
                        supportingText = { Text(ROLES.firstOrNull { it.first == rol }?.second ?: "") },
                        trailingIcon = { Icon(Iconos.Abajo, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(Modifier.matchParentSize().padding(top = 8.dp, bottom = 22.dp).clickable { abierto = true })
                    DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                        ROLES.forEach { (nombreRol, descripcion) ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(nombreRol, fontWeight = FontWeight.SemiBold)
                                        Text(descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = { rol = nombreRol; abierto = false }
                            )
                        }
                    }
                }
                if (esNuevo) {
                    Text(
                        "Al guardar, el sistema genera una contraseña temporal para entregarla al trabajador.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(
                onClick = { onGuardar(DatosUsuario(username, nombres, apellidos, dni, telefono, correo, rol)) },
                enabled = !guardando
            ) { Text(if (guardando) "Guardando…" else "Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar, enabled = !guardando) { Text("Cancelar") } }
    )
}
