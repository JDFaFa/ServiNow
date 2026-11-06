package com.example.servinow.domain.repository

import com.example.servinow.domain.model.Member

/** Identity comes from the authentication provider, never from local demo credentials. */
interface AuthenticationRepository {
    suspend fun currentMember(): Member?
    suspend fun signIn(email: String, password: String, moderator: Boolean = false): Member
    suspend fun register(name: String, email: String, password: String, confirmation: String)
    suspend fun sendRecovery(email: String, language: String)
    suspend fun signInWithGoogle(idToken: String): Member = error("Google is not configured")
    suspend fun deleteAccount(confirmation: String) { error("AUTH_DELETE_PENDING") }
    fun signOut()
}
