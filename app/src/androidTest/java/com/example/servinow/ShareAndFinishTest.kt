package com.example.servinow

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import com.example.servinow.domain.model.PostStatus
import com.example.servinow.features.community.CommunityViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ShareAndFinishTest {
    val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val rules: org.junit.rules.TestRule = org.junit.rules.RuleChain.outerRule(FreshDemoRule()).around(compose)
    private val vm get() = ViewModelProvider(compose.activity)[CommunityViewModel::class.java]
    private fun detail() {
        compose.waitUntil(15000) { vm.ready.value }
        compose.runOnIdle { vm.login("valentina@gmail.com", "ServiNow1!") }
        compose.waitUntil(15000) { vm.ui.value.page == "home" }
        compose.runOnIdle { vm.openPost("p1") }
        compose.waitUntil(15000) { vm.ui.value.page == "detail" }
    }
    @Test fun sharesPublicJobThroughAndroidChooser() {
        detail(); Intents.init()
        try {
            Intents.intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(ActivityResult(Activity.RESULT_CANCELED, null))
            compose.onNodeWithText("Compartir trabajo").performScrollTo().performClick()
            val chooser = Intents.getIntents().last { it.action == Intent.ACTION_CHOOSER }
            @Suppress("DEPRECATION") val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
            assertEquals(Intent.ACTION_SEND, send.action)
            assertEquals("text/plain", send.type)
            val text = send.getStringExtra(Intent.EXTRA_TEXT)!!
            assertTrue(text.contains(vm.state.value.posts.first { it.id == "p1" }.title))
            assertFalse(text.contains("valentina@gmail.com"))
        } finally { Intents.release() }
    }
    @Test fun authorCanFinishFromDetailAfterConfirmation() {
        detail()
        compose.onNodeWithText("Marcar como finalizado").performScrollTo().performClick()
        compose.waitUntil(15000) { vm.ui.value.page == "finish" }
        assertNotEquals(PostStatus.FINISHED, vm.state.value.posts.first { it.id == "p1" }.status)
        compose.onNodeWithText("Confirmar finalización").performScrollTo().performClick()
        compose.waitUntil(15000) { vm.state.value.posts.first { it.id == "p1" }.status == PostStatus.FINISHED }
    }
}
