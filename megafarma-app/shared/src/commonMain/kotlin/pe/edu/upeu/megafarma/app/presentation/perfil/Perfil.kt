package pe.edu.upeu.megafarma.app.presentation.perfil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import pe.edu.upeu.megafarma.app.domain.model.Sesion
import pe.edu.upeu.megafarma.app.domain.model.Usuario
import pe.edu.upeu.megafarma.app.domain.repository.AuthRepository
import pe.edu.upeu.megafarma.app.presentation.componentes.Avatar
import pe.edu.upeu.megafarma.app.presentation.componentes.Aviso
import pe.edu.upeu.megafarma.app.presentation.componentes.Cargando
import pe.edu.upeu.megafarma.app.presentation.componentes.Etiqueta
import pe.edu.upeu.megafarma.app.presentation.componentes.Iconos
import pe.edu.upeu.megafarma.app.presentation.componentes.MensajeError
import pe.edu.upeu.megafarma.app.presentation.componentes.Panel
import pe.edu.upeu.megafarma.app.presentation.tema.ColoresEstado

data class PerfilUiState(
    val cargando: Boolean = true,
    val usuario: Usuario? = null,
    val error: String? = null,
    val guardando: Boolean = false,
    val errorPassword: String? = null,
    val aviso: String? = null,
    /* Cambia cada vez que se guarda bien, para limpiar el formulario. */
    val guardados: Int = 0
)

/* Mi perfil: datos del usuario con la sesion abierta y cambio de contrasena. */
class PerfilViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _estado = MutableStateFlow(PerfilUiState())
    val estado: StateFlow<PerfilUiState> = _estado.asStateFlow()

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val r = auth.perfil()) {
                is Resultado.Exito -> _estado.update { it.copy(cargando = false, usuario = r.datos) }
                is Resultado.Error -> _estado.update { it.copy(cargando = false, error = r.mensaje) }
            }
        }
    }

    fun cambiarPassword(actual: String, nueva: String, confirmar: String) {
        val error = when {
            actual.isBlank() -> "Ingrese su contraseña actual"
            nueva.length < 8 -> "La nueva contraseña debe tener al menos 8 caracteres"
            nueva.none { it.isLetter() } || nueva.none { it.isDigit() } -> "La nueva contraseña debe tener al menos una letra y un número"
            nueva != confirmar -> "La confirmación no coincide con la nueva contraseña"
            nueva == actual -> "La nueva contraseña debe ser distinta de la actual"
            else -> null
        }
        if (error != null) {
            _estado.update { it.copy(errorPassword = error) }
            return
        }
        _estado.update { it.copy(guardando = true, errorPassword = null, aviso = null) }
        viewModelScope.launch {
            when (val r = auth.cambiarPassword(actual, nueva)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(guardando = false, aviso = "Contraseña actualizada", guardados = it.guardados + 1)
                }
                is Resultado.Error -> _estado.update { it.copy(guardando = false, errorPassword = r.mensaje) }
            }
        }
    }

    fun cerrarAviso() = _estado.update { it.copy(aviso = null) }
}

@Composable
fun PerfilScreen(sesion: Sesion, viewModel: PerfilViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargar() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        estado.aviso?.let { Aviso(it, false, viewModel::cerrarAviso) }

        Panel(Modifier.fillMaxWidth().widthIn(max = 720.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(sesion.nombreCompleto, 56)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(sesion.nombreCompleto, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(sesion.username, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Etiqueta(sesion.rol, ColoresEstado.info)
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            when {
                estado.cargando && estado.usuario == null -> Cargando()
                estado.error != null -> MensajeError(estado.error!!) { viewModel.cargar() }
                else -> estado.usuario?.let { u ->
                    Dato("Nombres", u.nombres ?: u.nombreCompleto)
                    Dato("Apellidos", u.apellidos)
                    Dato("DNI", u.dni)
                    Dato("Teléfono", u.telefono)
                    Dato("Correo", u.correo)
                    Text(
                        "Para corregir estos datos, pida al administrador que los actualice en Usuarios.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Panel(Modifier.fillMaxWidth().widthIn(max = 720.dp)) {
            Text("Cambiar contraseña", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Use al menos 8 caracteres, con letras y números.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            FormularioPassword(estado.guardando, estado.errorPassword, estado.guardados, viewModel::cambiarPassword)
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(etiqueta, Modifier.width(110.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor?.ifBlank { null } ?: "—", fontWeight = FontWeight.Medium)
    }
}

/*
 * Primer ingreso con una contrasena temporal (cuenta nueva o restablecida por el admin):
 * el trabajador debe elegir su propia contrasena antes de usar el sistema.
 */
@Composable
fun CambioPasswordObligatorioScreen(
    sesion: Sesion,
    onCerrarSesion: () -> Unit,
    viewModel: PerfilViewModel = koinViewModel()
) {
    val estado by viewModel.estado.collectAsState()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), contentAlignment = Alignment.Center) {
            Panel(Modifier.widthIn(max = 440.dp)) {
                Icon(Iconos.Candado, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(12.dp))
                Text("Cree su contraseña", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Hola ${sesion.nombreCompleto}. Ingresó con una contraseña temporal; elija una propia para continuar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                FormularioPassword(
                    estado.guardando, estado.errorPassword, estado.guardados, viewModel::cambiarPassword,
                    etiquetaActual = "Contraseña temporal"
                )
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onCerrarSesion) { Text("Cerrar sesión") }
            }
        }
    }
}

@Composable
private fun FormularioPassword(
    guardando: Boolean,
    error: String?,
    guardados: Int,
    onGuardar: (actual: String, nueva: String, confirmar: String) -> Unit,
    etiquetaActual: String = "Contraseña actual"
) {
    var actual by remember(guardados) { mutableStateOf("") }
    var nueva by remember(guardados) { mutableStateOf("") }
    var confirmar by remember(guardados) { mutableStateOf("") }
    var ver by remember { mutableStateOf(false) }
    val transformacion = if (ver) VisualTransformation.None else PasswordVisualTransformation()

    Column(Modifier.widthIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CampoClave(actual, { actual = it }, etiquetaActual, transformacion)
        CampoClave(nueva, { nueva = it }, "Nueva contraseña", transformacion)
        CampoClave(confirmar, { confirmar = it }, "Repetir nueva contraseña", transformacion)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = ver, onCheckedChange = { ver = it })
            Text("Mostrar contraseñas", style = MaterialTheme.typography.bodyMedium)
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        Button(onClick = { onGuardar(actual, nueva, confirmar) }, enabled = !guardando, modifier = Modifier.fillMaxWidth()) {
            Text(if (guardando) "Guardando…" else "Guardar contraseña")
        }
    }
}

@Composable
private fun CampoClave(valor: String, onCambio: (String) -> Unit, etiqueta: String, transformacion: VisualTransformation) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = true,
        visualTransformation = transformacion,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
}
