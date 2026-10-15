package com.example.servinow.features.explore
import com.example.servinow.domain.model.ExploreActivity
import com.example.servinow.domain.model.Job
import com.example.servinow.domain.model.SearchFilters
import com.example.servinow.domain.validation.filterJobs

import androidx.lifecycle.ViewModel
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.servinow.domain.repository.ExploreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ExploreState(val page: String = "home", val query: String = "",
    val filters: SearchFilters = SearchFilters(), val draft: SearchFilters = SearchFilters(),
    val selectedId: Int? = null, val message: String? = null,
    val interested: Set<Int> = emptySet(), val comments: Map<Int, List<String>> = emptyMap(),
    val commentDraft: String = "", val previousPage: String = "home", val catalog: List<Job> = emptyList()) {
    val jobs get() = catalog.map { it.copy(votes = it.votes + if (it.id in interested) 1 else 0) }
    val results get() = filterJobs(jobs, query, filters)
}
@HiltViewModel
class ExploreViewModel @Inject constructor(private val repository: ExploreRepository) : ViewModel() {
    private val _state = MutableStateFlow(ExploreState(catalog=repository.jobs()))
    val state = _state.asStateFlow()
    private val serviceHistory = mutableListOf<String>()
    init { val saved=repository.read(); _state.update { it.copy(interested=saved.interested,comments=saved.comments) } }
    private fun persist() {
        if(!repository.save(ExploreActivity(_state.value.interested,_state.value.comments))) message("No se pudieron guardar los cambios.")
    }
    fun openJob(id: Int) {
        if (_state.value.jobs.none { it.id == id }) return
        _state.update { it.copy(selectedId = id, previousPage = it.page, page = "detail", commentDraft = "") }
    }
    fun back() {
        if(serviceHistory.isNotEmpty()) { val previous=serviceHistory.removeAt(serviceHistory.lastIndex); _state.update { it.copy(page=previous) }; return }
        _state.update { it.copy(page = when (it.page) {
            "detail" -> it.previousPage
            "comments" -> "detail"
            "confirm-offer" -> "offers"
            "tracking" -> "offers"
            "finish", "review", "service-chat" -> "tracking"
            "my-offers" -> "worker"
            "offer" -> "detail"
            "profile" -> "worker"
            "cv-preview" -> "profile"
            "delete-post" -> "mine"
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
    fun page(page: String) {
        if(page=="home" || page=="mine") serviceHistory.clear()
        else if((page in com.example.servinow.features.services.servicePages || page in com.example.servinow.features.account.accountPages) && page != _state.value.page) serviceHistory.add(_state.value.page)
        _state.update { it.copy(page = page) }
    }
    fun query(value: String) { _state.update { it.copy(query = value) } }
    fun category(value: String) { _state.update { it.copy(filters = it.filters.copy(category = value)) } }
    fun editFilters() { _state.update { it.copy(page = "filters", draft = it.filters) } }
    fun draft(value: SearchFilters) { _state.update { it.copy(draft = value) } }
    fun apply() { _state.update { it.copy(filters = it.draft, page = "home") } }
    fun reset() { _state.update { it.copy(draft = SearchFilters(), query = "") } }
    fun select(id: Int) { _state.update { it.copy(selectedId = id) } }
    fun message(value: String?) { _state.update { it.copy(message = value) } }
}
