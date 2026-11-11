package com.example.servinow

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.servinow.features.community.CommunityViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import org.junit.*
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Opt-in real photo upload, Firestore persistence and a second user's read. */
class LivePhotoPersistenceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val vm get()=ViewModelProvider(compose.activity)[CommunityViewModel::class.java]
    private fun act(block:CommunityViewModel.()->Unit)=compose.runOnIdle { vm.block() }
    private fun <T> await(task:com.google.android.gms.tasks.Task<T>):T=Tasks.await(task,40,TimeUnit.SECONDS)
    @Test fun uploadPersistReadFromAnotherAccountAndRender() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("livePhotos")=="true")
        compose.waitUntil(30_000) { vm.ready.value }
        if (vm.state.value.me == null) {
            val credentials=File(compose.activity.filesDir,"photo-test-credentials.json")
            Assert.assertTrue("Sign in first or supply local test credentials",credentials.exists())
            val account=org.json.JSONObject(credentials.readText())
            act { login(account.getString("email"),account.getString("password")) }
        }
        val photo=File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir,"photo-upload-test.jpg")
        Assert.assertTrue("Provide a local test photo",photo.exists())
        compose.waitUntil(30_000) { vm.ready.value && vm.state.value.me!=null }
        val oldProfile=vm.state.value.me!!
        act {
            startPost()
            draft(ui.value.draft!!.copy(title="[PRUEBA FOTO] Reparar lavaplatos",description="Necesito revisar el lavaplatos de la cocina porque pierde agua.",budget=80000,imageUri=Uri.fromFile(photo).toString(),hasImage=true))
            go("preview")
        }
        compose.waitUntil(10_000) { vm.ui.value.page=="preview" }
        compose.onNodeWithText(compose.activity.getString(R.string.ui_submit_for_review_b370)).performScrollTo().performClick()
        compose.waitUntil(45_000) { vm.ui.value.page=="submitted" }
        val id=vm.ui.value.draft!!.id
        val db=FirebaseFirestore.getInstance()
        val post=db.collection("posts").document(id)
        val url=await(post.get(Source.SERVER)).getString("imageUri")!!
        Assert.assertTrue(url.startsWith("https://res.cloudinary.com/"))
        Assert.assertFalse(url.startsWith("file:"))
        await(post.update(mapOf("status" to "VERIFIED","updatedAt" to FieldValue.serverTimestamp())))
        val secondary=FirebaseApp.initializeApp(compose.activity,FirebaseApp.getInstance().options,"photo-check-${UUID.randomUUID()}")
        val other=FirebaseAuth.getInstance(secondary)
        try {
            await(other.createUserWithEmailAndPassword("derook7598+photo${System.currentTimeMillis()}@gmail.com","Photo!9${UUID.randomUUID()}"))
            val otherDb=FirebaseFirestore.getInstance(secondary)
            Assert.assertEquals(url,await(otherDb.collection("posts").document(id).get(Source.SERVER)).getString("imageUri"))
        } finally {
            other.currentUser?.let { await(it.delete()) }
            secondary.delete()
        }
        act { openPost(id) }
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("service-photo-loaded").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithTag("service-photo-loaded").onFirst().assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("service-photo-loaded").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle(); Thread.sleep(400)
        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.filesDir,"photo-cloud-loaded.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        // Profile save also uploads a local photo without a separate upload button.
        act { saveMember(oldProfile.copy(photoUri=Uri.fromFile(photo).toString())) }
        compose.waitUntil(45_000) { vm.state.value.me?.photoUri?.startsWith("https://res.cloudinary.com/")==true && !vm.ui.value.operationBusy }
        Assert.assertTrue(await(db.collection("profiles").document(oldProfile.id).get(Source.SERVER)).getString("photoUri")!!.startsWith("https://res.cloudinary.com/"))
        act { message(null); saveMember(oldProfile) }
        compose.waitUntil(30_000) { !vm.ui.value.operationBusy }
    }
}
