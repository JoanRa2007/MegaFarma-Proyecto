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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.megafarma.app.core.Resultado
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
    val porDesactivar: Usuario? = null
)

/* Administracion de usuarios (HU-22 a HU-24), solo ADMIN. */
class UsuariosViewModel(private val repositorio: UsuarioRepository) : ViewModel() {

    private val _estado = MutableStateFlow(UsuariosUiState())
    val estado: StateFlow<UsuariosUiState> = _estado.asStateFlow()

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val r = repositorio.listar()) {
                is Resultado.Exito -> _estado.update { it.copy(cargando = false, usuarios = r.datos) }
                is Resultado.Error -> _estado.update { it.copy(cargando = false, error = r.mensaje) }
            }
        }
    }

    fun nuevo() = _estado.update { it.copy(editando = Usuario(0, "", "", "VENDEDOR", true, false), errorFormulario = null) }

    fun editar(usuario: Usuario) = _estado.update { it.copy(editando = usuario, errorFormulario = null) }

    fun cerrarFormulario() = _estado.update { it.copy(editando = null) }

    fun cerrarAviso() = _estado.update { it.copy(aviso = null) }

    fun buscar(texto: String) = _estado.update { it.copy(texto = texto) }

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
            datos.username.trim().length < 3 -> "El usuario debe tener al menos 3 caracteres"
            datos.nombreCompleto.isBlank() -> "Ingrese el nombre completo"
            esNuevo && (datos.password?.length ?: 0) < 8 -> "La contraseña debe tener al menos 8 caracteres"
            !esNuevo && !datos.password.isNullOrEmpty() && datos.password.length < 8 -> "La nueva contraseña debe tener al menos 8 caracteres"
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
                    _estado.update {
                        it.copy(
                            guardando = false, editando = null, avisoEsError = false,
                            aviso = if (esNuevo) "Usuario ${r.datos.username} creado" else "Usuario ${r.datos.username} actualizado"
                        )
                    }
                    cargar()
                }
                is Resultado.Error -> _estado.update { it.copy(guardando = false, errorFormulario = r.mensaje) }
            }
        }
    }

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

    val q = estado.texto.trim().lowercase()
    val visibles = estado.usuarios.filter {
        q.isEmpty() || it.nombreCompleto.lowercase().contains(q) || it.username.lowercase().contains(q) || it.rol.lowercase().contains(q)
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = estado.texto,
                onValueChange = viewModel::buscar,
                placeholder = { Text("Buscar por nombre, usuario o rol") },
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
            visibles.isEmpty() -> Vacio(if (q.isEmpty()) "No hay usuarios registrados" else "Ningún usuario coincide con \"${estado.texto}\"")
            else -> {
                Text(
                    "${visibles.size} de ${estado.usuarios.size} usuario(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BoxWithConstraints(Modifier.weight(1f)) {
                    val ancho = maxWidth >= ANCHO_CELULAR
                    Tabla {
                        EncabezadoTabla(
                            if (ancho) listOf(Columna("Nombre", 2.4f), Columna("Usuario", 1.3f), Columna("Rol", 1.3f),
                                Columna("Estado", 1.4f), Columna("Acciones", 0.9f))
                            else listOf(Columna("Trabajador", 3f), Columna("", 0.9f))
                        )
                        LazyColumn {
                            items(visibles, key = { it.id }) { u ->
                                FilaUsuario(u, ancho, onEditar = { viewModel.editar(u) },
                                    onDesactivar = { viewModel.pedirDesactivar(u) }, onActivar = { viewModel.cambiarEstado(u) })
                            }
                        }
                    }
                }
            }
        }
    }

    estado.editando?.let { u ->
        UsuarioFormDialog(u, estado.guardando, estado.errorFormulario, viewModel::guardar, viewModel::cerrarFormulario)
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
private fun FilaUsuario(u: Usuario, ancho: Boolean, onEditar: () -> Unit, onDesactivar: () -> Unit, onActivar: () -> Unit) {
    FilaTabla {
        if (ancho) {
            Row(Modifier.weight(2.4f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(u.nombreCompleto, 32)
                Spacer(Modifier.width(10.dp))
                Text(u.nombreCompleto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Celda(u.username, 1.3f, secundario = true)
            Box(Modifier.weight(1.3f)) { Etiqueta(u.rol, colorRol(u.rol)) }
            Box(Modifier.weight(1.4f)) { EstadoUsuario(u) }
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
        Row(Modifier.weight(0.9f), verticalAlignment = Alignment.CenterVertically) {
            BotonAccion(Iconos.Editar, "Editar", ColoresEstado.alerta, onEditar)
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
    var username by remember(usuario.id) { mutableStateOf(usuario.username) }
    var nombre by remember(usuario.id) { mutableStateOf(usuario.nombreCompleto) }
    var clave by remember(usuario.id) { mutableStateOf("") }
    var rol by remember(usuario.id) { mutableStateOf(usuario.rol) }

    AlertDialog(
        onDismissRequest = { if (!guardando) onCerrar() },
        title = { Text(if (esNuevo) "Nuevo usuario" else "Editar ${usuario.username}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Campo(username, { username = it }, "Usuario *")
                Campo(nombre, { nombre = it }, "Nombre completo *")
                OutlinedTextField(
                    value = clave,
                    onValueChange = { clave = it },
                    label = { Text(if (esNuevo) "Contraseña *" else "Nueva contraseña (opcional)") },
                    supportingText = { Text("Mínimo 8 caracteres") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Rol", style = MaterialTheme.typography.labelLarge)
                ROLES.forEach { (nombreRol, descripcion) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = rol == nombreRol, onClick = { rol = nombreRol })
                        Column {
                            Text(nombreRol, fontWeight = FontWeight.SemiBold)
                            Text(descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = { onGuardar(DatosUsuario(username, clave.ifEmpty { null }, nombre, rol)) }, enabled = !guardando) {
                Text(if (guardando) "Guardando…" else "Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onCerrar, enabled = !guardando) { Text("Cancelar") } }
    )
}
