package com.example.servinow.domain.validation
import com.example.servinow.domain.model.*
import kotlin.math.*
fun distanceKm(job: Job, filters: SearchFilters): Double {
    val lat = Math.toRadians(job.latitude - filters.latitude)
    val lng = Math.toRadians(job.longitude - filters.longitude)
    val a = sin(lat / 2).pow(2) + cos(Math.toRadians(filters.latitude)) * cos(Math.toRadians(job.latitude)) * sin(lng / 2).pow(2)
    return 6371 * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
}
fun filterJobs(jobs: List<Job>, query: String, f: SearchFilters): List<Job> {
    val matches = jobs.filter { (f.category == "Todas" || it.category == f.category) &&
        (!f.openOnly || it.open) &&
        (f.area == "En mi ciudad · Bogotá" || distanceKm(it, f) <= 5) &&
        (query.isBlank() || "${it.title} ${it.category} ${it.area}".contains(query.trim(), ignoreCase = true)) }
    return when (f.order) {
        "Más relevantes · más votos" -> matches.sortedByDescending { it.votes }
        "Menor distancia" -> matches.sortedBy { distanceKm(it, f) }
        else -> matches.sortedBy { it.minutes }
    }
}
