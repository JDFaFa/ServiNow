package com.example.servinow.domain.repository

import com.example.servinow.domain.model.*
import kotlinx.coroutines.flow.StateFlow

/**
 * Datos de la fase académica: se reinician al cerrar el proceso. No representa una sesión remota.
 */
interface CommunityRepository {
    val state: StateFlow<CommunityState>

    fun login(email: String, password: String, moderator: Boolean = false): String?

    fun logout()

    fun register(name: String, email: String, password: String, confirmation: String): String?

    fun requestRecovery(email: String): String?

    fun resetPassword(email: String, password: String, confirmation: String): String?

    fun savePost(post: WorkPost): String?

    fun deletePost(id: String): String?

    fun verify(id: String): String?

    fun reject(id: String, reason: String): String?

    fun finish(id: String): String?

    fun vote(id: String): String?

    fun comment(id: String, text: String): String?

    fun offer(id: String, price: String, text: String, availability: String): String?

    fun accept(offerId: String): String?

    fun review(id: String, stars: Int, text: String): String?

    fun updateMember(member: Member): String?

    fun deleteAccount(confirmation: String): String?

    fun markNotice(id: String, read: Boolean)

    fun markAllNotices()
}
