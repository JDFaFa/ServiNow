package com.example.servinow.features.explore
import com.example.servinow.domain.model.Publication
import com.example.servinow.domain.validation.publicationError

import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import com.example.servinow.domain.repository.PublicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PublicationState(val posts: List<Publication> = emptyList(), val draft: Publication = Publication(),
    val editing: Boolean = false, val filter: String = "Todas", val message: String? = null)
@HiltViewModel
class PublicationViewModel @Inject constructor(private val repository: PublicationRepository) : ViewModel() {
    private val _state = MutableStateFlow(PublicationState(posts = repository.read()))
    val state = _state.asStateFlow()
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
        if (!repository.save(posts)) { message("No se pudo guardar. Inténtalo de nuevo."); return false }
        _state.update { it.copy(posts = posts, draft = p) }
        return true
    }
    fun delete(post: Publication) {
        val posts = _state.value.posts.filterNot { it.id == post.id }
        if (repository.save(posts)) _state.update { it.copy(posts = posts) } else message("No se pudo eliminar.")
    }
}
