package com.example.servinow.community

import com.example.servinow.data.repository.InMemoryCommunityRepository
import com.example.servinow.domain.model.*
import com.example.servinow.domain.repository.AuthenticationRepository
import com.example.servinow.features.community.CommunityViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteAuthenticationTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    private class Auth : AuthenticationRepository {
        var failure = false
        var signedOut = false
        var registered = false
        override suspend fun currentMember(): Member? = null
        override suspend fun signIn(email: String, password: String, moderator: Boolean): Member {
            if (failure) throw IllegalArgumentException("La contraseña es incorrecta. Inténtalo de nuevo.")
            return Member("firebase-uid", "Real User", email)
        }
        override suspend fun register(name: String,email: String,password: String,confirmation: String) { registered = true }
        override suspend fun sendRecovery(email: String, language: String) {}
        override fun signOut() { signedOut = true }
    }
    @Test fun providerFailureDoesNotFallBackToDemoCredentials() = runTest(dispatcher) {
        val repo = InMemoryCommunityRepository(); val auth = Auth().apply { failure = true }
        val vm = CommunityViewModel(repo, authentication = auth)
        vm.login("valentina@gmail.com", "ServiNow1!"); advanceUntilIdle()
        assertNull(repo.state.value.currentUser)
        assertNotNull(vm.ui.value.message)
        assertFalse(vm.ui.value.authBusy)
    }
    @Test fun providerUidBecomesActorAndLogoutClearsBothSessions() = runTest(dispatcher) {
        val repo = InMemoryCommunityRepository(); val auth = Auth()
        val vm = CommunityViewModel(repo, authentication = auth)
        vm.login("real@gmail.com", "Password1!"); advanceUntilIdle()
        assertEquals("firebase-uid",repo.state.value.currentUser)
        vm.logout(); advanceUntilIdle()
        assertTrue(auth.signedOut); assertNull(repo.state.value.currentUser)
    }
    @Test fun registrationUsesProviderWithoutCreatingLocalPassword() = runTest(dispatcher) {
        val repo=InMemoryCommunityRepository(); val auth=Auth()
        val vm=CommunityViewModel(repo,authentication=auth)
        vm.register("Real User","real@gmail.com","Password1!","Password1!"); advanceUntilIdle()
        assertTrue(auth.registered)
        assertNotNull(repo.login("real@gmail.com","Password1!"))
    }
    @Test fun remoteRecoveryNeverAllowsUnverifiedLocalPasswordReset() = runTest(dispatcher) {
        val repo=InMemoryCommunityRepository(); val vm=CommunityViewModel(repo,authentication=Auth())
        vm.reset("NewPassword1!","NewPassword1!")
        assertEquals("AUTH_RESET_BROWSER",vm.ui.value.message)
        assertNull(repo.login("valentina@gmail.com","ServiNow1!"))
    }
}
