package com.example.servinow.features.community

import androidx.lifecycle.ViewModel
import com.example.servinow.domain.model.*
import com.example.servinow.domain.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CommunityUi(
    val page: String = "login",
    val postId: String = "p1",
    val profileId: String = "andres",
    val offerId: String = "o1",
    val message: String? = null,
    val recoveryEmail: String = "",
    val draft: WorkPost? = null,
)

@HiltViewModel
class CommunityViewModel @Inject constructor(private val repository: CommunityRepository) :
    ViewModel() {
    val state = repository.state
    private val _ui = MutableStateFlow(CommunityUi())
    val ui = _ui.asStateFlow()
    private val history = mutableListOf<String>()

    fun go(page: String) {
        if (page == _ui.value.page) return
        if (page in setOf("home", "mine", "account", "notifications", "moderation", "login"))
            history.clear()
        else history.add(_ui.value.page)
        _ui.value = _ui.value.copy(page = page)
    }

    fun back() {
        _ui.value =
            _ui.value.copy(
                page =
                    if (history.isNotEmpty()) history.removeAt(history.lastIndex)
                    else if (state.value.me == null) "login"
                    else if (state.value.me?.role == MemberRole.MODERATOR) "moderation" else "home"
            )
    }

    fun message(text: String?) {
        _ui.value = _ui.value.copy(message = text)
    }

    private fun result(error: String?, next: String? = null, success: String? = null) {
        if (error != null) message(error)
        else {
            next?.let(::go)
            success?.let(::message)
        }
    }

    fun login(email: String, password: String, moderator: Boolean = false) {
        val error = repository.login(email, password, moderator)
        result(error, if (state.value.me?.role == MemberRole.MODERATOR) "moderation" else "home")
    }

    fun logout() {
        repository.logout()
        history.clear()
        go("login")
    }

    fun register(name: String, email: String, password: String, confirmation: String) {
        result(
            repository.register(name, email, password, confirmation),
            "login",
            "Cuenta creada en memoria. Ya puedes iniciar sesión.",
        )
    }

    fun recovery(email: String) {
        val error = repository.requestRecovery(email)
        if (error == null) _ui.value = _ui.value.copy(recoveryEmail = email.trim())
        result(error, "sent")
    }

    fun reset(password: String, confirmation: String) {
        result(
            repository.resetPassword(ui.value.recoveryEmail, password, confirmation),
            "login",
            "Contraseña actualizada en memoria. Ya puedes entrar.",
        )
    }

    fun openPost(id: String, page: String = "detail") {
        _ui.value = _ui.value.copy(postId = id)
        go(page)
    }

    fun profile(id: String) {
        _ui.value = _ui.value.copy(profileId = id)
        go("profile")
    }

    fun startPost() {
        _ui.value =
            _ui.value.copy(
                draft =
                    WorkPost(
                        UUID.randomUUID().toString(),
                        state.value.currentUser.orEmpty(),
                        "",
                        "Plomería",
                        "",
                        "Bogotá",
                        4.6486,
                        -74.0628,
                        0,
                        hasImage = false,
                    )
            )
        go("create")
    }

    fun editPost(p: WorkPost) {
        _ui.value = _ui.value.copy(draft = p)
        go("create")
    }

    fun draft(p: WorkPost) {
        _ui.value = _ui.value.copy(draft = p)
    }

    fun savePost() {
        val p = ui.value.draft ?: return
        val error = repository.savePost(p)
        if (error == null) _ui.value = _ui.value.copy(postId = p.id)
        result(error, "submitted")
    }

    fun deletePost() {
        result(repository.deletePost(ui.value.postId), "mine", "Publicación eliminada.")
    }

    fun verify() {
        result(
            repository.verify(ui.value.postId),
            "moderation",
            "Publicación verificada y visible en el feed.",
        )
    }

    fun reject(reason: String) {
        result(
            repository.reject(ui.value.postId, reason),
            "moderation",
            "Publicación rechazada. El autor recibió un aviso.",
        )
    }

    fun finish() {
        result(
            repository.finish(ui.value.postId),
            if (state.value.me?.role == MemberRole.MODERATOR) "moderation" else "tracking",
            "Trabajo finalizado.",
        )
    }

    fun vote() {
        result(repository.vote(ui.value.postId))
    }

    fun comment(text: String): Boolean {
        val error = repository.comment(ui.value.postId, text)
        result(error)
        return error == null
    }

    fun offer(price: String, text: String, availability: String) {
        result(
            repository.offer(ui.value.postId, price, text, availability),
            "my-offers",
            "Oferta enviada. El autor recibió un aviso.",
        )
    }

    fun selectOffer(id: String) {
        _ui.value = _ui.value.copy(offerId = id)
        go("confirm-offer")
    }

    fun accept() {
        result(
            repository.accept(ui.value.offerId),
            "tracking",
            "Trabajador asignado. Se generó su notificación.",
        )
    }

    fun review(stars: Int, text: String) {
        result(
            repository.review(ui.value.postId, stars, text),
            "tracking",
            "Reseña publicada en el perfil del trabajador.",
        )
    }

    fun saveMember(member: Member) {
        result(repository.updateMember(member), "account", "Perfil actualizado.")
    }

    fun deleteAccount(confirmation: String) {
        result(
            repository.deleteAccount(confirmation),
            "login",
            "Cuenta y datos asociados eliminados de la demostración.",
        )
    }

    fun mark(id: String, read: Boolean) = repository.markNotice(id, read)

    fun markAll() = repository.markAllNotices()
}
