package com.example.servinow

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.example.servinow.features.community.CommunityViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class MapSelectionTest {
    val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val rules: org.junit.rules.TestRule = org.junit.rules.RuleChain.outerRule(FreshDemoRule()).around(compose)
    private val vm get() = ViewModelProvider(compose.activity)[CommunityViewModel::class.java]
    private fun map(view: android.view.View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) map(view.getChildAt(i))?.let { return it }
        return null
    }
    @Test fun tappingFallbackMapUpdatesDraftAndSurvivesRotation() {
        compose.waitUntil(15000) { vm.ready.value }
        compose.runOnIdle { vm.login("andres@gmail.com", "ServiNow1!") }
        compose.waitUntil(15000) { vm.ui.value.page == "home" }
        compose.runOnIdle { vm.startPost(); vm.go("post-location") }
        compose.waitUntil(15000) { vm.ui.value.page == "post-location" }
        val ready = AtomicBoolean(false)
        compose.waitUntil(15000) {
            compose.activity.runOnUiThread { map(compose.activity.window.decorView)?.evaluateJavascript("typeof window.setPicked === 'function'") { ready.set(it == "true") } }
            ready.get()
        }
        val original = vm.ui.value.draft!!
        compose.onNodeWithTag("job-location-map").performScrollTo().performTouchInput { click(Offset(width * 0.7f, height * 0.35f)) }
        compose.waitUntil(10000) { vm.ui.value.draft!!.latitude != original.latitude }
        val selected = vm.ui.value.draft!!
        assertTrue(selected.latitude in -90.0..90.0)
        assertTrue(selected.longitude in -180.0..180.0)
        compose.activityRule.scenario.recreate()
        assertEquals(selected.latitude, vm.ui.value.draft!!.latitude, 0.000001)
        assertEquals(selected.longitude, vm.ui.value.draft!!.longitude, 0.000001)
    }
    @Test fun currentGpsLocationUpdatesDraft() {
        org.junit.Assume.assumeTrue("Requires host GPS injection", androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("gpsFix") == "true")
        val automation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.executeShellCommand("pm grant com.example.servinow android.permission.ACCESS_FINE_LOCATION").close()
        automation.executeShellCommand("pm grant com.example.servinow android.permission.ACCESS_COARSE_LOCATION").close()
        compose.waitUntil(15000) { vm.ready.value }
        compose.runOnIdle { vm.login("andres@gmail.com", "ServiNow1!") }
        compose.waitUntil(15000) { vm.ui.value.page == "home" }
        compose.runOnIdle { vm.startPost(); vm.go("post-location") }
        compose.waitUntil(15000) { vm.ui.value.page == "post-location" }
        compose.onNodeWithText("Mi ubicación").performScrollTo().performClick()
        // Host supplies this position through adb emu geo fix during the request.
        compose.waitUntil(25000) {
            kotlin.math.abs(vm.ui.value.draft!!.latitude - 4.65) < 0.0001 &&
                kotlin.math.abs(vm.ui.value.draft!!.longitude + 74.08) < 0.0001
        }
        compose.activityRule.scenario.recreate()
        assertEquals(4.65, vm.ui.value.draft!!.latitude, 0.0001)
        assertEquals(-74.08, vm.ui.value.draft!!.longitude, 0.0001)
    }

}
