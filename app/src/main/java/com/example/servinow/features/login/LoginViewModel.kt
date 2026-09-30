package com.example.servinow.features.login

import androidx.lifecycle.ViewModel
import com.example.servinow.core.util.EmailValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val rememberMe: Boolean = false,
    val submitted: Boolean = false,
    val message: String? = null,
) {
    val emailError: String?
        get() = if (submitted) EmailValidator.error(email) else null
    val passwordError: String?
        get() = if (submitted && password.isEmpty()) "Escribe tu contraseña" else null

}

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun onEmailChange(value: String) { _uiState.update { it.copy(email = value) } }
    fun onPasswordChange(value: String) { _uiState.update { it.copy(password = value) } }
    fun togglePassword() { _uiState.update { it.copy(passwordVisible = !it.passwordVisible) } }
    fun rememberMeChanged(value: Boolean) { _uiState.update { it.copy(rememberMe = value) } }
    fun dismissMessage() { _uiState.update { it.copy(message = null) } }
    fun showPendingFeature(name: String) {
        _uiState.update { it.copy(message = "$name se implementará en la siguiente etapa.") }
    }
    fun loginWithGoogle() {
        // Se conectará al proveedor elegido; nunca pedir la contraseña de Google aquí.
        _uiState.update { it.copy(message = "El acceso con Google aún requiere configurar el proveedor de autenticación.") }
    }
    // Invocar únicamente cuando el proveedor rechace las credenciales.
    fun onCredentialsRejected() {
        _uiState.update { it.copy(message = "La contraseña es incorrecta. Inténtalo de nuevo.") }
    }
    fun login() {
        _uiState.update { it.copy(submitted = true, email = it.email.trim()) }
        val state = _uiState.value
        val error = state.emailError ?: state.passwordError
        if (error != null) {
            _uiState.update { it.copy(message = error) }
            return
        }
        // Etapa de interfaz: no se simula una autenticación exitosa ni se registran contraseñas.
        _uiState.update { it.copy(message = "Formulario válido. Falta conectar el servicio de autenticación.") }
    }
}
