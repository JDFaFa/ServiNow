package com.example.servinow.domain.validation
import com.example.servinow.domain.model.*
fun proposalError(price: String, message: String, availability: String, duration: String): String? = when {
    (price.toLongOrNull() ?: 0) <= 0 -> "Escribe un precio entero mayor que cero."
    message.isBlank() || availability.isBlank() || duration.isBlank() -> "Completa qué incluye la oferta, disponibilidad y duración."
    message.length > 2000 || availability.length > 200 || duration.length > 100 -> "Reduce la longitud de los datos de tu oferta."
    else -> null
}
fun acceptProposal(data: ServiceData, id: String): ServiceData {
    val p = data.offers.find { it.id == id } ?: return data
    if (p.job in data.accepted || p.job in data.completed) return data
    return data.copy(accepted = data.accepted + (p.job to id))
}
fun finishService(data: ServiceData, job: String): ServiceData = if (job in data.accepted) data.copy(completed = data.completed + job) else data
fun addReview(data: ServiceData, review: ServiceReview): ServiceData {
    val assigned = data.offers.find { it.id == data.accepted[review.job] } ?: return data
    if (review.job !in data.completed || assigned.worker != review.worker || review.stars !in 1..5 || review.text.isBlank() || review.text.length > 1000 || data.reviews.any { it.job == review.job }) return data
    return data.copy(reviews = data.reviews + review)
}
