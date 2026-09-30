package com.example.servinow.features.explore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Publication(val id: String = UUID.randomUUID().toString(), val title: String = "",
    val category: String = "Plomería", val description: String = "", val budget: String = "",
    val photos: List<String> = emptyList(), val latitude: Double? = null, val longitude: Double? = null,
    val status: String = "Pendiente", val reason: String = "")
data class PublicationState(val posts: List<Publication> = emptyList(), val draft: Publication = Publication(),
    val editing: Boolean = false, val filter: String = "Todas", val message: String? = null)
class PublicationViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("publications_local", 0)
    private val _state = MutableStateFlow(PublicationState(posts = read()))
    val state = _state.asStateFlow()
    private fun read(): List<Publication> = runCatching {
        val a = JSONArray(prefs.getString("posts", "[]"))
        (0 until a.length()).map { i -> a.getJSONObject(i).let { o ->
            val photos = o.getJSONArray("photos")
            Publication(o.getString("id"), o.getString("title"), o.getString("category"), o.getString("description"),
                o.getString("budget"), (0 until photos.length()).map { photos.getString(it) },
                o.getDouble("lat"), o.getDouble("lng"), o.getString("status"), o.optString("reason"))
        } }
    }.getOrDefault(emptyList())
    private fun persist(posts: List<Publication>): Boolean = runCatching {
        val a = JSONArray()
        posts.forEach { p -> a.put(JSONObject().put("id",p.id).put("title",p.title).put("category",p.category)
            .put("description",p.description).put("budget",p.budget).put("photos",JSONArray(p.photos))
            .put("lat",p.latitude).put("lng",p.longitude).put("status",p.status).put("reason",p.reason)) }
        prefs.edit().putString("posts",a.toString()).commit()
    }.getOrDefault(false)
    fun start() { _state.update { it.copy(draft = Publication(), editing = false) } }
    fun edit(post: Publication) { _state.update { it.copy(draft = post, editing = true) } }
    fun change(post: Publication) { _state.update { it.copy(draft = post) } }
    fun filter(value: String) { _state.update { it.copy(filter = value) } }
    fun message(value: String?) { _state.update { it.copy(message = value) } }
    fun validate(step: Int): Boolean {
        val p = _state.value.draft
        val error = publicationError(p, step)
        if (error != null) message(error)
        return error == null
    }
    fun save(): Boolean {
        if (!validate(3)) return false
        val p = _state.value.draft.copy(title = _state.value.draft.title.trim(), status = "Pendiente", reason = "")
        val posts = _state.value.posts.filterNot { it.id == p.id } + p
        if (!persist(posts)) { message("No se pudo guardar. Inténtalo de nuevo."); return false }
        _state.update { it.copy(posts = posts, draft = p) }
        return true
    }
    fun delete(post: Publication) {
        val posts = _state.value.posts.filterNot { it.id == post.id }
        if (persist(posts)) _state.update { it.copy(posts = posts) } else message("No se pudo eliminar.")
    }
}
fun publicationError(p: Publication, step: Int): String? = when {
    p.title.isBlank() -> "Escribe un título para el trabajo."
    p.category !in categories.drop(1) -> "Selecciona una categoría."
    step >= 2 && p.description.isBlank() -> "Describe el trabajo que necesitas."
    step >= 2 && (p.budget.toLongOrNull() ?: 0) <= 0 -> "Escribe un presupuesto válido en pesos colombianos."
    step >= 2 && p.photos.isEmpty() -> "Agrega al menos una foto del trabajo."
    step >= 3 && (p.latitude == null || p.longitude == null || !p.latitude.isFinite() || !p.longitude.isFinite() || p.latitude !in -90.0..90.0 || p.longitude !in -180.0..180.0) -> "Selecciona la ubicación exacta en el mapa."
    else -> null
}
