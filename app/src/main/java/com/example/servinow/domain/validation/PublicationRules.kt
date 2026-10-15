package com.example.servinow.domain.validation
import com.example.servinow.domain.model.*
fun publicationError(p: Publication, step: Int): String? = when {
    p.title.isBlank() -> "Escribe un título para el trabajo."
    p.category !in categories.drop(1) -> "Selecciona una categoría."
    step >= 2 && p.description.isBlank() -> "Describe el trabajo que necesitas."
    step >= 2 && (p.budget.toLongOrNull() ?: 0) <= 0 -> "Escribe un presupuesto válido en pesos colombianos."
    step >= 2 && p.photos.isEmpty() -> "Agrega al menos una foto del trabajo."
    step >= 3 && (p.latitude == null || p.longitude == null || !p.latitude.isFinite() || !p.longitude.isFinite() || p.latitude !in -90.0..90.0 || p.longitude !in -180.0..180.0) -> "Selecciona la ubicación exacta en el mapa."
    else -> null
}
