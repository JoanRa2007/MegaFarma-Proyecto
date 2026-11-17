package pe.edu.upeu.megafarma.app.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.megafarma.app.core.Resultado
import pe.edu.upeu.megafarma.app.domain.repository.AuthRepository
import pe.edu.upeu.megafarma.app.domain.usecase.IniciarSesionUseCase

/* Estado de la pantalla de login (UiState). */
data class LoginUiState(
    val usuario: String = "",
    val clave: String = "",
    val servidor: String = "",
    val verServidor: Boolean = false,
    val cargando: Boolean = false,
    val error: String? = null
)

/*
 * MVVM: la pantalla solo dibuja el estado y avisa los eventos;
 * el ViewModel decide que hacer y actualiza el estado (StateFlow).
 */
class LoginViewModel(
    private val iniciarSesion: IniciarSesionUseCase,
    auth: AuthRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(LoginUiState(servidor = auth.servidor))
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    fun cambiarUsuario(valor: String) = _estado.update { it.copy(usuario = valor, error = null) }
    fun cambiarClave(valor: String) = _estado.update { it.copy(clave = valor, error = null) }
    fun cambiarServidor(valor: String) = _estado.update { it.copy(servidor = valor, error = null) }
    fun alternarServidor() = _estado.update { it.copy(verServidor = !it.verServidor) }

    fun ingresar() {
        val actual = _estado.value
        if (actual.cargando) return
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val resultado = iniciarSesion(actual.usuario, actual.clave, actual.servidor)) {
                // Al guardar la sesion, App cambia sola a la pantalla principal.
                is Resultado.Exito -> _estado.update { it.copy(cargando = false, clave = "") }
                is Resultado.Error -> _estado.update { it.copy(cargando = false, error = resultado.mensaje) }
            }
        }
    }
}
