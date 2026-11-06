package com.example.servinow.domain.repository

import com.example.servinow.domain.model.*
import kotlinx.coroutines.flow.StateFlow

/** Remote operations complete only after the server acknowledges them. */
interface RemoteCommunityRepository {
    val state: StateFlow<CommunityState>
    val error: StateFlow<String?>
    suspend fun connect(member: Member)
    fun disconnect()
    fun observePost(id: String)
    suspend fun savePost(post: WorkPost)
    suspend fun deletePost(id: String)
    suspend fun verify(id: String)
    suspend fun reject(id: String, reason: String)
    suspend fun finish(id: String)
    suspend fun vote(id: String)
    suspend fun comment(id: String, text: String)
    suspend fun offer(id: String, price: String, text: String, availability: String)
    suspend fun accept(offerId: String)
    suspend fun review(id: String, stars: Int, text: String)
    suspend fun updateMember(member: Member)
    suspend fun sendMessage(id: String, text: String)
    suspend fun markNotice(id: String, read: Boolean)
    suspend fun markAllNotices()
}
