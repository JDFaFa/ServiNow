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
    val operationBusy: Boolean = false,
    val authBusy: Boolean = false,
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
class CommunityViewModel @Inject constructor(private val repository: CommunityRepository, private val sessions: SessionRepository? = null, private val resources: com.example.servinow.core.util.ResourceProvider? = null, private val geocoder: com.example.servinow.domain.repository.GeocodingRepository? = null, private val authentication: com.example.servinow.domain.repository.AuthenticationRepository? = null, private val remote: com.example.servinow.domain.repository.RemoteCommunityRepository? = null) :
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
                val restored = if (authentication != null) {
                    repository.logout()
                    if (!prefs.rememberSession) authentication.signOut()
                    authentication.currentMember()?.also { member ->
                        repository.setAuthenticatedMember(member)
                        remote?.connect(member)
                    } != null
                } else com.example.servinow.domain.usecase.auth.RestoreSessionUseCase(repository, sessions)(prefs.session)
                initialDestination = if (restored) { if (repository.state.value.me?.role == MemberRole.MODERATOR) "moderation" else "home" } else if (prefs.welcomeSeen) "login" else "welcome"
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { initialDestination = "login" }
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
    val state = remote?.state ?: repository.state
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
        if(page in setOf("detail","tracking","chat","comments")) remote?.observePost(_ui.value.postId)
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

    val usesRemoteAuthentication get() = authentication != null

    private fun authOperation(recovery: Boolean = false, block: suspend () -> Unit) {
        if (_ui.value.authBusy) return
        _ui.value = _ui.value.copy(authBusy = true)
        viewModelScope.launch {
            try { block() }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) {
                val text = when (error) {
                    is IllegalArgumentException -> error.message ?: "AUTH_FAILED"
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException,
                    is com.google.firebase.auth.FirebaseAuthInvalidUserException -> if (recovery) "AUTH_RECOVERY_FAILED" else "AUTH_INVALID_CREDENTIALS"
                    is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "Ya existe una cuenta con este correo."
                    is com.google.firebase.FirebaseNetworkException -> "AUTH_NETWORK"
                    is com.google.firebase.FirebaseTooManyRequestsException -> "AUTH_THROTTLED"
                    else -> if (recovery) "AUTH_RECOVERY_FAILED" else "AUTH_FAILED"
                }
                message(text)
            } finally { _ui.value = _ui.value.copy(authBusy = false) }
        }
    }

    fun login(email: String, password: String, moderator: Boolean = false) {
        authentication?.let { auth ->
            authOperation {
                val member = auth.signIn(email, password, moderator)
                repository.setAuthenticatedMember(member)
                remote?.connect(member)
                go(if (member.role == MemberRole.MODERATOR) "moderation" else "home")
            }
            return
        }
        val error = repository.login(email, password, moderator)
        if (error != null) { message(error); return }
        viewModelScope.launch {
            try { state.value.me?.let { sessions?.saveSession(UserSession(it.id, it.role)) } }
            catch (_: java.io.IOException) { message("Entraste, pero no se pudo recordar la sesión.") }
            go(if (state.value.me?.role == MemberRole.MODERATOR) "moderation" else "home")
        }
    }

    fun googleToken(idToken: String) {
        val auth=authentication ?: return
        authOperation {
            val member=auth.signInWithGoogle(idToken)
            repository.setAuthenticatedMember(member)
            remote?.connect(member)
            go(if(member.role==MemberRole.MODERATOR) "moderation" else "home")
        }
    }
    fun logout() {
        viewModelScope.launch {
            try { sessions?.saveSession(null) }
            catch (_: java.io.IOException) { message("No se pudo cerrar la sesión guardada. Inténtalo de nuevo."); return@launch }
            remote?.disconnect()
            authentication?.signOut()
            repository.logout()
            go("login")
        }
    }

    fun register(name: String, email: String, password: String, confirmation: String) {
        authentication?.let { auth ->
            authOperation { auth.register(name, email, password, confirmation); go("login"); message("AUTH_CREATED") }
            return
        }
        result(
            repository.register(name, email, password, confirmation),
            "login",
            "Cuenta creada en memoria. Ya puedes iniciar sesión.",
        )
    }

    fun recovery(email: String) {
        authentication?.let { auth ->
            authOperation(recovery = true) {
                val language = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().get(0)?.language ?: java.util.Locale.getDefault().language
                auth.sendRecovery(email, language)
                _ui.value = _ui.value.copy(recoveryEmail = email.trim())
                go("sent")
            }
            return
        }
        val error = repository.requestRecovery(email)
        if (error == null) _ui.value = _ui.value.copy(recoveryEmail = email.trim())
        result(error, "sent")
    }

    fun reset(password: String, confirmation: String) {
        if (authentication != null) { message("AUTH_RESET_BROWSER"); return }
        result(
            repository.resetPassword(ui.value.recoveryEmail, password, confirmation),
            "login",
            "Contraseña actualizada en memoria. Ya puedes entrar.",
        )
    }

    fun openPost(id: String, page: String = "detail") {
        _ui.value = _ui.value.copy(postId = id)
        remote?.observePost(id)
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
                        "Armenia",
                        4.535,
                        -75.675,
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

    private fun operation(next: String? = null, success: String? = null, completed: () -> Unit = {}, action: suspend () -> String?) {
        if (_ui.value.operationBusy) return
        _ui.value = _ui.value.copy(operationBusy = true)
        viewModelScope.launch {
            try {
                val error=action()
                if(error==null) completed()
                result(error,next,success)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) {
                message(when {
                    error.message in setOf("IMAGE_SIZE", "IMAGE_TYPE", "IMAGE_UNREADABLE", "IMAGE_UPLOAD_FAILED") -> "POST_IMAGE_FAILED"
                    error is com.google.firebase.firestore.FirebaseFirestoreException && error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED -> "POST_PERMISSION_FAILED"
                    else -> "CLOUD_OPERATION_FAILED"
                })
            }
            finally { _ui.value = _ui.value.copy(operationBusy=false) }
        }
    }
    fun savePost() {
        val p=ui.value.draft ?: return
        operation("submitted",completed={ _ui.value=_ui.value.copy(postId=p.id) }) {
            if(remote!=null) { remote.savePost(p); null } else repository.savePost(p)
        }
    }
    fun deletePost() = operation("mine", "Publicación eliminada.") {
        if(remote!=null) { remote.deletePost(ui.value.postId); null } else repository.deletePost(ui.value.postId)
    }
    fun verify() = operation("moderation", "Publicación verificada y visible en el feed.") {
        if(remote!=null) { remote.verify(ui.value.postId); null } else repository.verify(ui.value.postId)
    }
    fun reject(reason:String) = operation("moderation") {
        if(remote!=null) { remote.reject(ui.value.postId,reason); null } else repository.reject(ui.value.postId,reason)
    }
    fun finish() = operation("tracking", "Trabajo finalizado.") {
        if(remote!=null) { remote.finish(ui.value.postId); null } else repository.finish(ui.value.postId)
    }
    fun vote() = operation {
        if(remote!=null) { remote.vote(ui.value.postId); null } else repository.vote(ui.value.postId)
    }
    fun comment(text:String, onSent:()->Unit = {}) = operation(completed=onSent) {
        if(remote!=null) { remote.comment(ui.value.postId,text); null } else repository.comment(ui.value.postId,text)
    }
    fun offer(price:String,text:String,availability:String) = operation("my-offers") {
        if(remote!=null) { remote.offer(ui.value.postId,price,text,availability); null } else repository.offer(ui.value.postId,price,text,availability)
    }

    fun selectOffer(id: String) {
        _ui.value = _ui.value.copy(offerId = id)
        go("confirm-offer")
    }

    fun accept() = operation("tracking") {
        if(remote!=null) { remote.accept(ui.value.offerId); null } else repository.accept(ui.value.offerId)
    }
    fun review(stars:Int,text:String) = operation("tracking", "Reseña publicada en el perfil del trabajador.") {
        if(remote!=null) { remote.review(ui.value.postId,stars,text); null } else repository.review(ui.value.postId,stars,text)
    }
    fun saveMember(member:Member) = operation("account", "Perfil actualizado.") {
        if(remote!=null) { remote.updateMember(member); null } else repository.updateMember(member)
    }

    fun deleteAccount(confirmation: String) {
        if (authentication != null) {
            authOperation {
                authentication.deleteAccount(confirmation)
                remote?.disconnect()
                repository.logout()
                sessions?.saveSession(null)
                go("login")
                message("ACCOUNT_DELETED")
            }
            return
        }
        val error = repository.deleteAccount(confirmation)
        if (error != null) { message(error); return }
        viewModelScope.launch {
            try { sessions?.saveSession(null) } catch (_: java.io.IOException) { /* Missing accounts cannot be restored. */ }
            go("login")
            message("Cuenta y datos asociados eliminados de la demostración.")
        }
    }

    fun sendMessage(text:String,onSent:()->Unit = {}) = operation(completed=onSent) {
        if(remote!=null) { remote.sendMessage(ui.value.postId,text); null } else repository.sendMessage(ui.value.postId,text)
    }
    fun mark(id:String,read:Boolean) = operation {
        if(remote!=null) remote.markNotice(id,read) else repository.markNotice(id,read)
        null
    }
    fun markAll() = operation {
        if(remote!=null) remote.markAllNotices() else repository.markAllNotices()
        null
    }
    val remoteError = remote?.error ?: MutableStateFlow<String?>(null)
    override fun onCleared() { remote?.disconnect(); super.onCleared() }
}
