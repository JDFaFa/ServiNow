package com.example.servinow

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.servinow.features.community.CommunityViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import org.json.JSONObject
import org.junit.*
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class LiveAcceptedChatTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val vm get()=ViewModelProvider(compose.activity)[CommunityViewModel::class.java]
    private fun act(block:CommunityViewModel.()->Unit)=compose.runOnIdle { vm.block() }
    private fun <T> await(task:com.google.android.gms.tasks.Task<T>):T=Tasks.await(task,40,TimeUnit.SECONDS)
    private fun at(route:String)=compose.waitUntil(30_000) { vm.ui.value.page==route }
    private fun click(id:Int) { val node=compose.onAllNodesWithText(compose.activity.getString(id)).onLast(); if(!node.isDisplayed())node.performScrollTo();node.performClick() }
    private fun login(email:String,password:String) {
        act { message(null); logout() };at("login")
        act { login(email,password) }
        compose.waitUntil(40_000) { vm.state.value.me?.email==email && !vm.ui.value.authBusy }
    }
    private fun send(text:String) {
        compose.onNode(hasText(compose.activity.getString(R.string.chat_message)) and hasSetTextAction()).performScrollTo().performTextReplacement(text)
        click(R.string.chat_send)
        compose.waitUntil(30_000) { vm.state.value.messages.any { it.text==text } }
    }
    @Test fun acceptanceOpensLiveChatForBothParticipants() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("liveChat")=="true")
        val credentials=File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir,"chat-test-admin.json")
        Assert.assertTrue(credentials.exists())
        val admin=JSONObject(credentials.readText())
        compose.waitUntil(30_000) { vm.ready.value }
        login(admin.getString("email"),admin.getString("password"))
        val options=FirebaseApp.getInstance().options
        val tag=System.currentTimeMillis().toString()
        val clientApp=FirebaseApp.initializeApp(compose.activity,options,"client-$tag")
        val workerApp=FirebaseApp.initializeApp(compose.activity,options,"worker-$tag")
        val ca=FirebaseAuth.getInstance(clientApp);val wa=FirebaseAuth.getInstance(workerApp)
        val ce="derook7598+chatclient$tag@gmail.com"; val we="derook7598+chatworker$tag@gmail.com"
        val cp="Test!9${UUID.randomUUID()}";val wp="Test!9${UUID.randomUUID()}"
        try {
            val cid=await(ca.createUserWithEmailAndPassword(ce,cp)).user!!.uid
            val wid=await(wa.createUserWithEmailAndPassword(we,wp)).user!!.uid
            val cdb=FirebaseFirestore.getInstance(clientApp);val wdb=FirebaseFirestore.getInstance(workerApp)
            val id="qa_chat_$tag"
            await(cdb.collection("posts").document(id).set(mapOf("author" to cid,"title" to "[PRUEBA CHAT] Reparar puerta","description" to "Necesito ajustar las bisagras de la puerta de madera.","category" to "Carpintería","city" to "Armenia","latitude" to 4.535,"longitude" to -75.675,"budget" to 90000L,"imageUri" to null,"status" to "PENDING","assigned" to null,"createdAt" to FieldValue.serverTimestamp(),"updatedAt" to FieldValue.serverTimestamp())))
            await(FirebaseFirestore.getInstance().collection("posts").document(id).update(mapOf("status" to "VERIFIED","updatedAt" to FieldValue.serverTimestamp())))
            val offer="${id}_$wid"
            await(wdb.collection("offers").document(offer).set(mapOf("post" to id,"author" to cid,"worker" to wid,"price" to 80000L,"description" to "[PRUEBA] Ajuste de bisagras.","availability" to "Mañana","createdAt" to FieldValue.serverTimestamp())))
            login(ce,cp)
            compose.waitUntil(20_000) { vm.state.value.posts.any { it.id==id } }
            act { openPost(id,"tracking") };at("tracking")
            compose.onNodeWithText(compose.activity.getString(R.string.private_chat)).assertDoesNotExist()
            // Acceptance arrives while tracking is already open: reproduce the stale-listener bug.
            await(cdb.collection("posts").document(id).update(mapOf("status" to "IN_PROGRESS","assigned" to wid,"updatedAt" to FieldValue.serverTimestamp())))
            val initial="[PRUEBA] Hola, ya aceptaste mi propuesta."
            await(wdb.collection("posts").document(id).collection("messages").add(mapOf("sender" to wid,"recipient" to cid,"text" to initial,"createdAt" to FieldValue.serverTimestamp())))
            compose.waitUntil(30_000) { vm.state.value.messages.any { it.text==initial } }
            click(R.string.private_chat);at("chat")
            send("[PRUEBA] Sí, te espero mañana.")
            login(we,wp)
            act { go("my-offers") };at("my-offers")
            compose.waitUntil(20_000) { vm.state.value.posts.any { it.id==id && it.assigned==wid } && vm.state.value.offers.any { it.id==offer } }
            click(R.string.private_chat);at("chat")
            compose.waitUntil(30_000) { vm.state.value.messages.any { it.text=="[PRUEBA] Sí, te espero mañana." } }
            send("[PRUEBA] Perfecto, llevaré las herramientas.")
            login(ce,cp)
            act { openPost(id) };at("detail")
            click(R.string.private_chat);at("chat")
            compose.waitUntil(30_000) { vm.state.value.messages.size==3 }
            compose.activityRule.scenario.recreate();at("chat")
            compose.waitUntil(30_000) { vm.state.value.messages.size==3 }
            compose.waitForIdle();Thread.sleep(400)
            val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            File(compose.activity.filesDir,"chat-dos-cuentas.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        } finally {
            login(admin.getString("email"),admin.getString("password"))
            ca.currentUser?.let { await(it.delete()) };wa.currentUser?.let { await(it.delete()) }
            clientApp.delete();workerApp.delete()
        }
    }
}
