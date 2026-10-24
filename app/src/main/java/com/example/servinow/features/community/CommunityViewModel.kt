package com.example.servinow.features.community

import com.example.servinow.navigation.NavigationEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servinow.domain.repository.SessionRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.example.servinow.domain.model.*
import com.example.servinow.domain.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CommunityUi(
    val address: String? = null,
    val addressLoading: Boolean = false,
    val page: String = "welcome",
    val postId: String = "p1",
    val profileId: String = "andres",
    val offerId: String = "o1",
    val message: String? = null,
    val recoveryEmail: String = "",
    val draft: WorkPost? = null,
)

@HiltViewModel
class CommunityViewModel @Inject constructor(private val repository: CommunityRepository, private val sessions: SessionRepository? = null, private val resources: com.example.servinow.core.util.ResourceProvider? = null, private val geocoder: com.example.servinow.domain.repository.GeocodingRepository? = null) :
    ViewModel() {
    private val _ready = MutableStateFlow(sessions == null)
    val ready = _ready.asStateFlow()
    private val _preferences = MutableStateFlow(AppPreferences())
    val preferences = _preferences.asStateFlow()
    var initialDestination: String = "welcome"
        private set
    init {
        if (sessions != null) viewModelScope.launch {
            try {
                val prefs = sessions.preferences.first()
                val restored = com.example.servinow.domain.usecase.auth.RestoreSessionUseCase(repository, sessions)(prefs.session)
                initialDestination = if (restored) { if (repository.state.value.me?.role == MemberRole.MODERATOR) "moderation" else "home" } else if (prefs.welcomeSeen) "login" else "welcome"
            } catch (_: java.io.IOException) { initialDestination = "login" }
            _ready.value = true
            sessions.preferences.collect { _preferences.value = it }
        }
    }
    fun finishWelcome(destination: String) {
        viewModelScope.launch {
            try { sessions?.setWelcomeSeen() } catch (_: java.io.IOException) { /* Navigation remains available. */ }
            go(destination)
        }
    }
    fun rememberSession(enabled: Boolean) {
        viewModelScope.launch {
            try {
                sessions?.setRememberSession(enabled)
                if (enabled) state.value.me?.let { sessions?.saveSession(UserSession(it.id, it.role)) }
            } catch (_: java.io.IOException) { message("No se pudo guardar la preferencia. Inténtalo de nuevo.") }
        }
    }
    val state = repository.state
    private val _ui = MutableStateFlow(CommunityUi())
    val ui = _ui.asStateFlow()
    private val navigationEvents = Channel<NavigationEvent>(Channel.UNLIMITED)
    val navigation = navigationEvents.receiveAsFlow()

    fun go(page: String) {
        navigationEvents.trySend(NavigationEvent.Open(page))
    }

    fun back() {
        navigationEvents.trySend(NavigationEvent.Back)
    }

    // This is a reflection of NavController, not a second navigation stack.
    fun destinationChanged(page: String) {
        _ui.value = _ui.value.copy(page = page)
    }

    fun message(text: String?) {
        _ui.value = _ui.value.copy(message = text?.let { resources?.translate(it) ?: it })
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
        if (error != null) { message(error); return }
        viewModelScope.launch {
            try { state.value.me?.let { sessions?.saveSession(UserSession(it.id, it.role)) } }
            catch (_: java.io.IOException) { message("Entraste, pero no se pudo recordar la sesión.") }
            go(if (state.value.me?.role == MemberRole.MODERATOR) "moderation" else "home")
        }
    }

    fun logout() {
        viewModelScope.launch {
            try { sessions?.saveSession(null) }
            catch (_: java.io.IOException) { message("No se pudo cerrar la sesión guardada. Inténtalo de nuevo."); return@launch }
            repository.logout()
            go("login")
        }
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

    private var addressRequest: kotlinx.coroutines.Job? = null
    fun lookupAddress(latitude: Double, longitude: Double) {
        addressRequest?.cancel()
        _ui.value = _ui.value.copy(address = null, addressLoading = true)
        addressRequest = viewModelScope.launch {
            kotlinx.coroutines.delay(350)
            val text = try { geocoder?.address(latitude, longitude) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { null }
            _ui.value = _ui.value.copy(address = text, addressLoading = false)
        }
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
        val error = repository.deleteAccount(confirmation)
        if (error != null) { message(error); return }
        viewModelScope.launch {
            try { sessions?.saveSession(null) } catch (_: java.io.IOException) { /* Missing accounts cannot be restored. */ }
            go("login")
            message("Cuenta y datos asociados eliminados de la demostración.")
        }
    }

    fun mark(id: String, read: Boolean) = repository.markNotice(id, read)

    fun markAll() = repository.markAllNotices()
}
