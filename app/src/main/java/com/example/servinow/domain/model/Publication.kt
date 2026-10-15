package com.example.servinow.domain.model
import java.util.UUID
data class Publication(val id: String = UUID.randomUUID().toString(), val title: String = "",
    val category: String = "Plomería", val description: String = "", val budget: String = "",
    val photos: List<String> = emptyList(), val latitude: Double? = null, val longitude: Double? = null,
    val status: String = "Pendiente", val reason: String = "")
