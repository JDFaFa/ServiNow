package com.example.servinow.domain.validation

fun offerInputError(price: String, description: String, availability: String): String? = when {
    price.trim().toLongOrNull()?.let { it in 1..1_000_000_000L } != true -> "OFFER_PRICE_INVALID"
    description.trim().length < 5 -> "OFFER_DESCRIPTION_SHORT"
    description.trim().length > 1000 -> "OFFER_DESCRIPTION_INVALID"
    availability.trim().length !in 1..200 -> "OFFER_AVAILABILITY_INVALID"
    else -> null
}
