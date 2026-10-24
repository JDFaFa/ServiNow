package com.example.servinow.community

import com.example.servinow.domain.model.*
import com.example.servinow.domain.repository.SessionRepository
import com.example.servinow.domain.usecase.auth.RestoreSessionUseCase
import com.example.servinow.data.repository.InMemoryCommunityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SessionTest {
    private class Sessions : SessionRepository {
        override val preferences = MutableStateFlow(AppPreferences())
        override suspend fun saveSession(session: UserSession?) { preferences.value = preferences.value.copy(session = session) }
        override suspend fun setWelcomeSeen(seen: Boolean) { preferences.value = preferences.value.copy(welcomeSeen = seen) }
        override suspend fun setRememberSession(enabled: Boolean) { preferences.value = preferences.value.copy(rememberSession = enabled) }
    }
    @Test fun validSessionRestoresKnownUser() = runBlocking {
        val repo = InMemoryCommunityRepository(); val sessions = Sessions()
        assertTrue(RestoreSessionUseCase(repo, sessions)(UserSession("andres", MemberRole.USER)))
        assertEquals("andres", repo.state.value.currentUser)
    }
    @Test fun invalidRoleCannotElevatePermissions() = runBlocking {
        val repo = InMemoryCommunityRepository(); val sessions = Sessions()
        sessions.saveSession(UserSession("andres", MemberRole.MODERATOR))
        assertFalse(RestoreSessionUseCase(repo, sessions)(sessions.preferences.value.session))
        assertNull(repo.state.value.currentUser); assertNull(sessions.preferences.value.session)
    }
    @Test fun missingMemoryAccountClearsStaleSession() = runBlocking {
        val original = InMemoryCommunityRepository()
        original.register("New Member", "new@gmail.com", "Valid123!", "Valid123!")
        original.login("new@gmail.com", "Valid123!")
        val sessions = Sessions(); sessions.saveSession(UserSession(original.state.value.me!!.id, MemberRole.USER))
        val fresh = InMemoryCommunityRepository()
        assertFalse(RestoreSessionUseCase(fresh, sessions)(sessions.preferences.value.session))
        assertNull(sessions.preferences.value.session)
    }
    @Test fun absentSessionDoesNotSignIn() = runBlocking {
        val repo = InMemoryCommunityRepository()
        assertFalse(RestoreSessionUseCase(repo, Sessions())(null))
        assertNull(repo.state.value.me)
    }
    @Test fun profilePhotoIsStoredAndCannotModifyAnotherUser() {
        val repo = InMemoryCommunityRepository(); repo.login("andres@gmail.com", "ServiNow1!")
        val changed = repo.state.value.me!!.copy(photoUri = "file:///private/photo.jpg")
        assertNull(repo.updateMember(changed)); assertEquals(changed.photoUri, repo.state.value.me!!.photoUri)
        assertNotNull(repo.updateMember(changed.copy(id = "valentina")))
    }
    @Test fun selectedPhotoSurvivesPostSaveAndEdit() {
        val repo = InMemoryCommunityRepository(); repo.login("valentina@gmail.com", "ServiNow1!")
        val post = WorkPost("photo-job", "valentina", "Repair kitchen", "Plomería", "Repair the kitchen sink", "Bogotá", 4.6, -74.0, 50000, imageUri = "file:///private/job.jpg")
        assertNull(repo.savePost(post))
        assertEquals(post.imageUri, repo.state.value.posts.last().imageUri)
        assertNull(repo.savePost(post.copy(description = "Repair the kitchen sink and tap")))
        assertEquals(post.imageUri, repo.state.value.posts.last().imageUri)
    }
}
