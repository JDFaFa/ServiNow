package com.example.servinow.features.register

/** Regla exclusiva del registro. Mostrar el resultado en un diálogo al enviar el formulario. */
object RegistrationPasswordValidator {
    fun error(password: String): String? = when {
        password.length < 8 || password.none { it.isUpperCase() } ||
            password.none { it.isDigit() } ||
            password.none { !it.isLetterOrDigit() && !it.isWhitespace() } ->
            "La contraseña debe tener mínimo 8 caracteres, una mayúscula, un número y un carácter especial."
        else -> null
    }
}
