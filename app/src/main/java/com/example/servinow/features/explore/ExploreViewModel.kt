package com.example.servinow.features.explore

import androidx.lifecycle.ViewModel
import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.*

data class Job(val id: Int, val title: String, val category: String, val area: String,
    val price: Int, val minutes: Int, val votes: Int, val latitude: Double, val longitude: Double,
    val open: Boolean = true)
data class SearchFilters(val category: String = "Todas", val area: String = "Cerca de mí · hasta 5 km",
    val order: String = "Más recientes", val openOnly: Boolean = true,
    val latitude: Double = 4.6486, val longitude: Double = -74.0628)
val categories = listOf("Todas", "Plomería", "Electricidad", "Carpintería", "Mantenimiento", "Pintura", "Limpieza", "Mecánica", "Jardinería", "Climatización", "Cerrajería", "Mudanzas")
val exampleJobs = listOf(
    Job(1,"Reparar fuga debajo del lavaplatos","Plomería","Chapinero, Bogotá",120000,25,12,4.659,-74.062),
    Job(2,"Instalar dos lámparas de techo","Electricidad","Teusaquillo, Bogotá",90000,60,8,4.63,-74.074),
    Job(3,"Ajustar puertas del armario","Carpintería","Chapinero, Bogotá",150000,120,5,4.664,-74.056))
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
data class ExploreState(val page: String = "home", val query: String = "",
    val filters: SearchFilters = SearchFilters(), val draft: SearchFilters = SearchFilters(),
    val selectedId: Int? = null, val message: String? = null,
    val interested: Set<Int> = emptySet(), val comments: Map<Int, List<String>> = emptyMap(),
    val commentDraft: String = "", val previousPage: String = "home") {
    val jobs get() = exampleJobs.map { it.copy(votes = it.votes + if (it.id in interested) 1 else 0) }
    val results get() = filterJobs(jobs, query, filters)
}
class ExploreViewModel : ViewModel() {
    private val _state = MutableStateFlow(ExploreState())
    val state = _state.asStateFlow()
    private var storage: SharedPreferences? = null
    fun attachStorage(context: Context) {
        if (storage != null) return
        val prefs = context.getSharedPreferences("explore_local", Context.MODE_PRIVATE)
        storage = prefs
        val votes = prefs.getStringSet("interested", emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
        val comments = runCatching {
            val json = JSONObject(prefs.getString("comments", "{}") ?: "{}")
            json.keys().asSequence().mapNotNull { key -> key.toIntOrNull()?.let { id ->
                val values = json.getJSONArray(key)
                id to (0 until values.length()).map { values.getString(it) }
            } }.toMap()
        }.getOrDefault(emptyMap())
        _state.update { it.copy(interested = votes, comments = comments) }
    }
    private fun persist() {
        val prefs = storage ?: return
        val json = JSONObject()
        _state.value.comments.forEach { (id, values) -> json.put(id.toString(), JSONArray(values)) }
        prefs.edit().putStringSet("interested", _state.value.interested.map { it.toString() }.toSet())
            ?.putString("comments", json.toString())?.apply()
    }
    fun openJob(id: Int) {
        if (exampleJobs.none { it.id == id }) return
        _state.update { it.copy(selectedId = id, previousPage = it.page, page = "detail", commentDraft = "") }
    }
    fun back() { _state.update { it.copy(page = when (it.page) {
            "detail" -> it.previousPage
            "comments" -> "detail"
            "create-details" -> "create"
            "post-location" -> "create-details"
            "preview" -> "post-location"
            "edit", "rejected", "submitted" -> "mine"
            else -> "home"
        }) } }
    fun toggleInterest() {
        val id = _state.value.selectedId ?: return
        _state.update { it.copy(interested = if (id in it.interested) it.interested - id else it.interested + id) }
        persist()
    }
    fun comment(value: String) { _state.update { it.copy(commentDraft = value) } }
    fun postComment() {
        val state = _state.value
        val id = state.selectedId ?: return
        val value = state.commentDraft.trim()
        if (value.isEmpty()) { message("Escribe un comentario antes de publicar."); return }
        if (value.length > 1000) { message("El comentario puede tener hasta 1000 caracteres."); return }
        _state.update { it.copy(comments = it.comments + (id to (it.comments[id].orEmpty() + value)), commentDraft = "") }
        persist()
    }
    fun page(page: String) { _state.update { it.copy(page = page) } }
    fun query(value: String) { _state.update { it.copy(query = value) } }
    fun category(value: String) { _state.update { it.copy(filters = it.filters.copy(category = value)) } }
    fun editFilters() { _state.update { it.copy(page = "filters", draft = it.filters) } }
    fun draft(value: SearchFilters) { _state.update { it.copy(draft = value) } }
    fun apply() { _state.update { it.copy(filters = it.draft, page = "home") } }
    fun reset() { _state.update { it.copy(draft = SearchFilters(), query = "") } }
    fun select(id: Int) { _state.update { it.copy(selectedId = id) } }
    fun message(value: String?) { _state.update { it.copy(message = value) } }
}
