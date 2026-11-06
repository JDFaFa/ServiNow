package com.example.servinow.data.cloud

import com.example.servinow.core.util.EmailValidator
import com.example.servinow.domain.model.Member
import com.example.servinow.domain.model.MemberRole
import com.example.servinow.domain.repository.AuthenticationRepository
import com.example.servinow.domain.validation.RegistrationPasswordValidator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthenticationRepository @Inject constructor(private val gateway: CloudGateway) : AuthenticationRepository {
    private val auth get() = FirebaseAuth.getInstance()
    private suspend fun member(user: FirebaseUser): Member {
        val claims = user.getIdToken(false).waitResult().claims
        val moderator=claims["moderator"] == true || gateway.isModerator()
        return Member(user.uid, user.displayName ?: user.email?.substringBefore('@').orEmpty(),
            user.email.orEmpty(), if (moderator) MemberRole.MODERATOR else MemberRole.USER)
    }
    override suspend fun currentMember(): Member? = auth.currentUser?.let { member(it) }
    override suspend fun signIn(email: String, password: String, moderator: Boolean): Member {
        EmailValidator.error(email)?.let { throw IllegalArgumentException(it) }
        require(password.isNotEmpty()) { "La contraseña es incorrecta. Inténtalo de nuevo." }
        val user = auth.signInWithEmailAndPassword(email.trim(), password).waitResult().user ?: error("AUTH_FAILED")
        return try {
            member(user).also {
                if (moderator && it.role != MemberRole.MODERATOR) {
                    throw IllegalArgumentException("Esta cuenta no tiene permisos de moderador.")
                }
            }
        } catch (error: Exception) { auth.signOut(); throw error }
    }
    override suspend fun register(name: String, email: String, password: String, confirmation: String) {
        require(name.trim().length in 2..100) { "Escribe un nombre de 2 a 100 caracteres." }
        EmailValidator.error(email)?.let { throw IllegalArgumentException(it) }
        RegistrationPasswordValidator.error(password)?.let { throw IllegalArgumentException(it) }
        require(password == confirmation) { "Las contraseñas no coinciden." }
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).waitResult().user ?: error("AUTH_FAILED")
        try {
            user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).waitResult()
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { throw IllegalArgumentException("AUTH_CREATED_PROFILE_PENDING") }
        finally { auth.signOut() }
    }
    override suspend fun sendRecovery(email: String, language: String) {
        EmailValidator.error(email)?.let { throw IllegalArgumentException(it) }
        auth.setLanguageCode(if (language.startsWith("en")) "en" else "es")
        auth.sendPasswordResetEmail(email.trim()).waitResult()
    }
    override suspend fun signInWithGoogle(idToken: String): Member {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        val user = auth.signInWithCredential(credential).waitResult().user ?: error("AUTH_FAILED")
        return member(user)
    }
    override suspend fun deleteAccount(confirmation: String) {
        require(confirmation == "ELIMINAR") { "DELETE_CONFIRMATION" }
        try { gateway.deleteAccount(confirmation) }
        catch(e:kotlinx.coroutines.CancellationException) { throw e }
        catch(e:Exception) { throw IllegalArgumentException(e.message ?: "ACCOUNT_CLEANUP_FAILED") }
        signOut()
    }
    override fun signOut() {
        auth.signOut()
        com.google.firebase.messaging.FirebaseMessaging.getInstance().deleteToken()
    }
}
