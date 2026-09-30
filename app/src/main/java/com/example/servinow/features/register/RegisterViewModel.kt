package com.example.servinow.features.register

import androidx.lifecycle.ViewModel
import com.example.servinow.core.util.EmailValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmation: String = "",
    val passwordVisible: Boolean = false,
    val confirmationVisible: Boolean = false,
    val message: String? = null,
    val choosingLocation: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationConfirmed: Boolean = false,
)

class RegisterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()
    fun nameChanged(value: String) { _uiState.update { it.copy(name = value) } }
    fun emailChanged(value: String) { _uiState.update { it.copy(email = value) } }
    fun passwordChanged(value: String) { _uiState.update { it.copy(password = value) } }
    fun confirmationChanged(value: String) { _uiState.update { it.copy(confirmation = value) } }
    fun togglePassword() { _uiState.update { it.copy(passwordVisible = !it.passwordVisible) } }
    fun toggleConfirmation() { _uiState.update { it.copy(confirmationVisible = !it.confirmationVisible) } }
    fun dismissMessage() { _uiState.update { it.copy(message = null) } }
    fun google() { _uiState.update { it.copy(message = "El registro con Google estará disponible cuando conectemos el servicio de autenticación.") } }
    fun backToForm() { _uiState.update { it.copy(choosingLocation = false) } }
    fun selectLocation(latitude: Double, longitude: Double) {
        if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return
        _uiState.update { it.copy(latitude = latitude, longitude = longitude, locationConfirmed = false) }
    }
    fun confirmLocation() {
        val state = _uiState.value
        if (state.latitude == null || state.longitude == null) {
            _uiState.update { it.copy(message = "Toca el mapa para seleccionar tu dirección.") }
            return
        }
        _uiState.update { it.copy(locationConfirmed = true, message = "Ubicación confirmada para este registro. Por ahora, el formulario no crea una cuenta ni guarda los datos en una base de datos.") }
    }
    fun submit() {
        val state = _uiState.value
        val error = when {
            state.name.isBlank() -> "Escribe tu nombre completo."
            else -> EmailValidator.error(state.email)
                ?: RegistrationPasswordValidator.error(state.password)
                ?: if (state.confirmation != state.password) "Las contraseñas no coinciden." else null
        }
        _uiState.update { it.copy(message = error, choosingLocation = error == null) }
    }
}
