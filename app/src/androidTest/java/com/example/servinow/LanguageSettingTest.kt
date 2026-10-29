package com.example.servinow

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.core.os.LocaleListCompat
import com.example.servinow.features.community.CommunityViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LanguageSettingTest {
    val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val rules: org.junit.rules.TestRule = org.junit.rules.RuleChain.outerRule(FreshDemoRule()).around(compose)
    private val vm get() = ViewModelProvider(compose.activity)[CommunityViewModel::class.java]
    @Test fun accountLanguageChangesAndSurvivesRecreation() {
        compose.waitUntil(15000) { vm.ready.value }
        compose.runOnIdle { vm.login("andres@gmail.com", "ServiNow1!") }
        compose.waitUntil(15000) { vm.ui.value.page == "home" }
        compose.runOnIdle { vm.go("account") }
        try {
            compose.onNodeWithText("Idioma:", substring = true).performScrollTo().performClick()
            compose.onNodeWithText("English", useUnmergedTree = true).performClick()
            compose.waitUntil(15000) {
                runCatching { compose.onAllNodesWithText("Language: English").fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
            }
            assertEquals("andres", vm.state.value.me?.id)
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText("Language: English").performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText("Español", useUnmergedTree = true).performClick()
            compose.waitUntil(15000) {
                runCatching { compose.onAllNodesWithText("Idioma: Español").fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
            }
            assertEquals("es", AppCompatDelegate.getApplicationLocales().toLanguageTags())
        } finally {
            compose.activity.runOnUiThread { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es")) }
        }
    }
}
