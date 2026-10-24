package com.example.servinow

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.example.servinow.features.community.CommunityViewModel
import com.example.servinow.data.repository.local.SessionRepositoryImpl
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Run prepare and restoreAndLogout in separate instrumentation processes. */
class SessionRestartTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val vm get() = ViewModelProvider(compose.activity)[CommunityViewModel::class.java]
    @Test fun prepare() {
        compose.waitUntil(15000) { vm.ready.value }
        runBlocking { SessionRepositoryImpl(compose.activity.applicationContext).setRememberSession(true) }
        compose.runOnIdle { vm.login("andres@gmail.com", "ServiNow1!") }
        compose.waitUntil(15000) { vm.ui.value.page == "home" }
        assertEquals("andres", runBlocking { SessionRepositoryImpl(compose.activity.applicationContext).preferences.first().session?.userId })
    }
    @Test fun restoreAndLogout() {
        compose.waitUntil(15000) { vm.ready.value }
        compose.waitUntil(15000) { vm.ui.value.page == "home" }
        assertEquals("home", vm.initialDestination)
        assertEquals("andres", vm.state.value.me?.id)
        compose.runOnIdle { vm.logout() }
        compose.waitUntil(15000) { vm.ui.value.page == "login" }
        assertNull(runBlocking { SessionRepositoryImpl(compose.activity.applicationContext).preferences.first().session })
    }
}
