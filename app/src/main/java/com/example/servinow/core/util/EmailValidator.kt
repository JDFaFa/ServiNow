package com.example.servinow.core.util

import java.util.Locale

/** Comprueba el formato y el dominio; no verifica existencia ni propiedad de la cuenta. */
object EmailValidator {
    private val allowedDomains = setOf("gmail.com", "hotmail.com", "outlook.com", "yahoo.com")
    private val localPattern = Regex("^[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+$")

    fun error(value: String): String? {
        val email = value.trim()
        if (email.isEmpty()) return "Escribe tu correo electrónico"
        val parts = email.split('@')
        if (parts.size != 2) return "Escribe un correo válido, como nombre@gmail.com"
        val local = parts[0]
        val domain = parts[1].lowercase(Locale.ROOT)
        if (email.length > 254 || local.length !in 1..64 || !localPattern.matches(local) ||
            local.startsWith('.') || local.endsWith('.') || ".." in local) {
            return "Escribe un correo válido, como nombre@gmail.com"
        }
        if (domain !in allowedDomains) {
            return "Solo se admiten correos @gmail.com, @hotmail.com, @outlook.com o @yahoo.com."
        }
        return null
    }
}
