package com.example.servinow

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.example.servinow.features.community.CommunityViewModel
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PhotoPickerTest {
    val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val rules: org.junit.rules.TestRule = org.junit.rules.RuleChain.outerRule(FreshDemoRule()).around(compose)
    private val vm get() = ViewModelProvider(compose.activity)[CommunityViewModel::class.java]
    private fun click(text: String) {
        val node = compose.onAllNodesWithText(text).onLast()
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
    }
    private fun openProfile() {
        compose.waitUntil(15000) { vm.ready.value }
        compose.runOnIdle { vm.login("andres@gmail.com", "ServiNow1!") }
        compose.waitUntil(15000) { vm.ui.value.page == "home" }
        compose.runOnIdle { vm.go("edit-account") }
        compose.waitUntil(15000) { vm.ui.value.page == "edit-account" }
    }
    private fun picture(): Uri {
        val context = compose.activity
        val photo = File(File(context.filesDir, "photos").apply { mkdirs() }, "test-source.png")
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLUE)
        photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photo)
    }
    @Test fun galleryPhotoIsCopiedAndSavedOnProfile() {
        openProfile(); Intents.init()
        try {
            val source = picture()
            Intents.intending(hasAction(Intent.ACTION_GET_CONTENT)).respondWith(ActivityResult(Activity.RESULT_OK, Intent().setData(source)))
            click("Seleccionar foto"); click("Elegir de la galería")
            compose.waitUntil(10000) { compose.onAllNodesWithText("Quitar foto").fetchSemanticsNodes().isNotEmpty() }
            click("Guardar cambios")
            compose.waitUntil(10000) { vm.state.value.me?.photoUri != null }
            val stored = vm.state.value.me!!.photoUri!!
            assertTrue(File(Uri.parse(stored).path!!).length() > 0)
            assertNotEquals(source.toString(), stored)
        } finally { Intents.release() }
    }
    @Test fun cancellingGalleryKeepsExistingPhoto() {
        openProfile()
        val original = vm.state.value.me?.photoUri
        Intents.init()
        try {
            Intents.intending(hasAction(Intent.ACTION_GET_CONTENT)).respondWith(ActivityResult(Activity.RESULT_CANCELED, null))
            click("Seleccionar foto"); click("Elegir de la galería")
            compose.onNodeWithText("Seleccionar foto").assertExists()
            if (original == null) compose.onNodeWithText("Quitar foto").assertDoesNotExist()
            else compose.onNodeWithText("Quitar foto").assertExists()
            assertEquals(original, vm.state.value.me?.photoUri)
        } finally { Intents.release() }
    }
    @Test fun deniedCameraStillAllowsGallery() {
        openProfile(); click("Seleccionar foto"); click("Tomar foto")
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val deny = device.wait(Until.findObject(By.text(java.util.regex.Pattern.compile("(?i)(don.t allow|no permitir)"))), 8000)
        // A prior permanent rejection can return immediately without another system dialog.
        deny?.click()
        compose.waitUntil(15000) {
            runCatching {
                compose.onAllNodesWithText("No se permitió usar la cámara. Puedes elegir una foto de la galería o habilitar el permiso en Ajustes.")
                    .fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        compose.onNodeWithText("No se permitió usar la cámara. Puedes elegir una foto de la galería o habilitar el permiso en Ajustes.").assertIsDisplayed()
        click("Entendido"); click("Seleccionar foto")
        compose.onNodeWithText("Elegir de la galería").assertIsDisplayed()
    }
}
