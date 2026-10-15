package com.example.servinow.domain.model
data class Job(val id: Int, val title: String, val category: String, val area: String,
    val price: Int, val minutes: Int, val votes: Int, val latitude: Double, val longitude: Double,
    val open: Boolean = true)
data class SearchFilters(val category: String = "Todas", val area: String = "Cerca de mí · hasta 5 km",
    val order: String = "Más recientes", val openOnly: Boolean = true,
    val latitude: Double = 4.6486, val longitude: Double = -74.0628)
val categories = listOf("Todas", "Plomería", "Electricidad", "Carpintería", "Mantenimiento", "Pintura", "Limpieza", "Mecánica", "Jardinería", "Climatización", "Cerrajería", "Mudanzas")

data class ExploreActivity(val interested:Set<Int> = emptySet(), val comments:Map<Int,List<String>> = emptyMap())
