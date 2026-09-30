package com.example.servinow.features.recovery

import androidx.lifecycle.ViewModel
import com.example.servinow.core.util.EmailValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class RecoveryStep { EMAIL, SENT, NEW_PASSWORD }

data class RecoveryUiState(val email: String = "", val message: String? = null,
    val password: String = "", val confirmation: String = "",
    val step: RecoveryStep = RecoveryStep.EMAIL)

class RecoveryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RecoveryUiState())
    val uiState = _uiState.asStateFlow()
    fun restart() { _uiState.update { RecoveryUiState(email = it.email) } }
    fun back() { _uiState.update { it.copy(step = if (it.step == RecoveryStep.NEW_PASSWORD) RecoveryStep.SENT else RecoveryStep.EMAIL, message = null, password = "", confirmation = "") } }
    fun previewLink() { _uiState.update { if (it.step == RecoveryStep.SENT) it.copy(step = RecoveryStep.NEW_PASSWORD) else it } }
    fun resend() { _uiState.update { it.copy(message = "En este prototipo no se envían correos. El reenvío estará disponible al conectar la autenticación.") } }
    fun emailChanged(value: String) { _uiState.update { it.copy(email = value) } }
    fun dismissMessage() { _uiState.update { it.copy(message = null) } }
    fun passwordChanged(value: String) { _uiState.update { it.copy(password = value) } }
    fun confirmationChanged(value: String) { _uiState.update { it.copy(confirmation = value) } }
    fun notifications() { _uiState.update { it.copy(message = "Inicia sesión para consultar tus avisos.") } }
    fun savePassword() {
        val state = _uiState.value
        val error = com.example.servinow.features.register.RegistrationPasswordValidator.error(state.password)
            ?: if (state.password != state.confirmation) "Las contraseñas no coinciden." else null
        _uiState.update { it.copy(message = error ?: "La contraseña es válida, pero aún no se ha guardado. Falta conectar la recuperación mediante un enlace seguro enviado a tu correo.") }
    }
    fun submit() {
        val email = _uiState.value.email.trim()
        val error = EmailValidator.error(email)
        _uiState.update { it.copy(email = email, message = error,
            step = if (error == null) RecoveryStep.SENT else RecoveryStep.EMAIL) }
    }
}
