package com.example.servinow

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.example.servinow.features.community.CommunityViewModel
import com.google.firebase.auth.FirebaseAuth
import org.junit.*

/** Opt in with -e liveAi true, using the account already signed into the emulator. */
class LiveAiFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val vm get() = ViewModelProvider(compose.activity)[CommunityViewModel::class.java]
    private fun text(id:Int)=compose.activity.getString(id)
    private fun click(id:Int) {
        val node=compose.onAllNodesWithText(text(id)).onLast()
        if(id==R.string.ai_improve) node.performScrollTo()
        node.performClick()
    }
    private fun act(block:CommunityViewModel.()->Unit)=compose.runOnIdle { vm.block() }
    @Test fun improvePreviewApplyAndPreserveInvalidDraft() {
        Assume.assumeTrue(androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("liveAi")=="true")
        Assume.assumeNotNull(FirebaseAuth.getInstance().currentUser)
        compose.waitUntil(30_000) { vm.ready.value && vm.state.value.me != null }
        act { startPost(); draft(ui.value.draft!!.copy(title="Reparar cocina",description="se daño la llave dela cosina bota agua")); go("create-details") }
        compose.waitUntil(10_000) { vm.ui.value.page=="create-details" }
        val original=vm.ui.value.draft!!.description
        click(R.string.ai_improve); click(R.string.chat_send)
        compose.waitUntil(50_000) { compose.onAllNodesWithText(text(R.string.ai_use)).fetchSemanticsNodes().isNotEmpty() }
        Assert.assertEquals(original,vm.ui.value.draft!!.description)
        click(R.string.ai_use)
        compose.waitUntil(5_000) { vm.ui.value.draft!!.description!=original }
        val improved=vm.ui.value.draft!!.description
        Assert.assertTrue("Suggestion: $improved", improved.contains("cocina",ignoreCase=true))
        Assert.assertFalse(improved.contains("cosina"))
        compose.waitForIdle()
        Thread.sleep(500) // Wait for the dialog exit animation before capturing evidence.
        val bitmap=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(compose.activity.filesDir,"ai-aplicada.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        act { draft(ui.value.draft!!.copy(description="qeqeqqewfaaa")) }
        click(R.string.ai_improve); click(R.string.chat_send)
        compose.waitUntil(15_000) { compose.onAllNodesWithText(text(R.string.ai_needs_details)).fetchSemanticsNodes().isNotEmpty() }
        Assert.assertEquals("qeqeqqewfaaa",vm.ui.value.draft!!.description)
        act { draft(ui.value.draft!!.copy(description=improved)) }
    }
}
