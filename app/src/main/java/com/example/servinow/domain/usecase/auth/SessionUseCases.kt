package com.example.servinow.domain.usecase.auth

import com.example.servinow.domain.model.*
import com.example.servinow.domain.repository.*
import javax.inject.Inject

class ObserveSessionUseCase @Inject constructor(private val sessions: SessionRepository) {
    operator fun invoke() = sessions.preferences
}
class RestoreSessionUseCase @Inject constructor(private val community: CommunityRepository, private val sessions: SessionRepository) {
    suspend operator fun invoke(session: UserSession?): Boolean {
        community.logout()
        if (session == null) return false
        val restored = community.restoreSession(session)
        if (!restored) sessions.saveSession(null)
        return restored
    }
}
