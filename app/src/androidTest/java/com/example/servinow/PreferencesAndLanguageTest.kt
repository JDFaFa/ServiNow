package com.example.servinow

import android.app.LocaleManager
import android.os.LocaleList
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.example.servinow.data.repository.local.SessionRepositoryImpl
import com.example.servinow.domain.model.*
import com.example.servinow.features.community.CommunityViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PreferencesAndLanguageTest {
    val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val rules: org.junit.rules.TestRule = org.junit.rules.RuleChain.outerRule(FreshDemoRule()).around(compose)
    private val vm get() = ViewModelProvider(compose.activity)[CommunityViewModel::class.java]
    @Test fun dataStorePersistsAndClearsOnlySessionFields() = runBlocking {
        val sessions = SessionRepositoryImpl(compose.activity.applicationContext)
        sessions.setWelcomeSeen(); sessions.saveSession(UserSession("andres", MemberRole.USER))
        val second = SessionRepositoryImpl(compose.activity.applicationContext)
        assertEquals(UserSession("andres", MemberRole.USER), second.preferences.first().session)
        sessions.setRememberSession(false)
        assertNull(second.preferences.first().session)
        assertTrue(second.preferences.first().welcomeSeen)
        sessions.saveSession(UserSession("andres", MemberRole.USER))
        assertNull(second.preferences.first().session)
        sessions.setRememberSession(true); sessions.saveSession(UserSession("moderador", MemberRole.MODERATOR))
        assertEquals(MemberRole.MODERATOR, second.preferences.first().session!!.role)
        sessions.saveSession(null); assertNull(second.preferences.first().session)
    }
    @Test fun englishLocaleShowsTranslatedWelcomeAndValidation() {
        compose.runOnIdle {
            compose.activity.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags("en")
        }
        compose.waitUntil(15000) { compose.onAllNodesWithText("Get started").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Get started").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Already have an account? Sign in").performScrollTo().performClick()
        compose.onNodeWithText("Welcome back.").assertIsDisplayed()
        compose.onNodeWithText("New here? Create an account").performScrollTo().performClick()
        compose.onAllNodesWithText("Create account").onLast().performScrollTo().performClick()
        compose.onNodeWithText("Enter a name between 2 and 100 characters.").assertIsDisplayed()
    }
    @Test fun successfulLoginStoresSessionAndLogoutRemovesIt() {
        compose.waitUntil(15000) { vm.ready.value }
        compose.runOnIdle { vm.login("andres@gmail.com", "ServiNow1!") }
        compose.waitUntil(15000) { vm.ui.value.page == "home" }
        val sessions = SessionRepositoryImpl(compose.activity.applicationContext)
        assertEquals("andres", runBlocking { sessions.preferences.first().session?.userId })
        compose.runOnIdle { vm.logout() }
        compose.waitUntil(15000) { vm.ui.value.page == "login" }
        assertNull(runBlocking { sessions.preferences.first().session })
    }
}
