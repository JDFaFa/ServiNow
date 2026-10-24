package com.example.servinow.data.repository.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.servinow.domain.model.*
import com.example.servinow.domain.repository.SessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "session_preferences")
@Singleton
class SessionRepositoryImpl @Inject constructor(@ApplicationContext private val context: Context) : SessionRepository {
    private val user = stringPreferencesKey("user_id")
    private val role = stringPreferencesKey("role")
    private val welcome = booleanPreferencesKey("welcome_seen")
    private val remember = booleanPreferencesKey("remember_session")
    override val preferences = context.sessionDataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { p ->
        val r = MemberRole.entries.find { it.name == p[role] }
        val id = p[user]?.takeIf { it.isNotBlank() }
        AppPreferences(if (id != null && r != null && p[remember] != false) UserSession(id, r) else null, p[welcome] ?: false, p[remember] ?: true)
    }
    override suspend fun saveSession(session: UserSession?) {
        context.sessionDataStore.edit { p ->
            p.remove(user); p.remove(role)
            if (session != null && p[remember] != false) { p[user] = session.userId; p[role] = session.role.name }
        }
    }
    override suspend fun setWelcomeSeen(seen: Boolean) { context.sessionDataStore.edit { it[welcome] = seen } }
    override suspend fun setRememberSession(enabled: Boolean) {
        context.sessionDataStore.edit { p ->
            p[remember] = enabled
            if (!enabled) { p.remove(user); p.remove(role) }
        }
    }
}
