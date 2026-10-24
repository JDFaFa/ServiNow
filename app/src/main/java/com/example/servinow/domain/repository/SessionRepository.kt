package com.example.servinow.domain.repository

import com.example.servinow.domain.model.*
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    val preferences: Flow<AppPreferences>
    suspend fun saveSession(session: UserSession?)
    suspend fun setWelcomeSeen(seen: Boolean = true)
    suspend fun setRememberSession(enabled: Boolean)
}
