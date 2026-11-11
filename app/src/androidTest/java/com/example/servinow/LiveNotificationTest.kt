package com.example.servinow

import android.app.NotificationManager
import android.app.Notification
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.example.servinow.features.community.CommunityViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import org.junit.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit

class LiveNotificationTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val vm get()=ViewModelProvider(compose.activity)[CommunityViewModel::class.java]
    private fun act(block:CommunityViewModel.()->Unit)=compose.runOnIdle { vm.block() }
    private fun <T> await(task:com.google.android.gms.tasks.Task<T>):T=Tasks.await(task,40,TimeUnit.SECONDS)
    private fun notifyAs(user:com.google.firebase.auth.FirebaseUser,type:String,post:String,event:String):JSONObject {
        val connection=URL(compose.activity.getString(R.string.services_base_url)+"/v1/notify").openConnection() as HttpURLConnection
        try {
            connection.requestMethod="POST";connection.doOutput=true;connection.readTimeout=30000
            connection.setRequestProperty("Authorization","Bearer "+await(user.getIdToken(false)).token)
            connection.setRequestProperty("Content-Type","application/json")
            connection.outputStream.use { it.write(JSONObject().put("type",type).put("post",post).put("event",event).toString().toByteArray()) }
            Assert.assertEquals(200,connection.responseCode)
            return JSONObject(connection.inputStream.bufferedReader().readText())
        } finally { connection.disconnect() }
    }
    @Test fun foregroundBackgroundAndTapOpenCorrectConversation() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("liveNotifications")=="true")
        compose.waitUntil(30_000) { vm.ready.value }
        val file=File(compose.activity.filesDir,"notification-test-admin.json")
        Assert.assertTrue(file.exists());val account=JSONObject(file.readText())
        act { logout() };compose.waitUntil(10_000) { vm.ui.value.page=="login" }
        act { login(account.getString("email"),account.getString("password")) }
        compose.waitUntil(40_000) { vm.state.value.me!=null&&!vm.ui.value.authBusy }
        val client=FirebaseAuth.getInstance().currentUser!!;val db=FirebaseFirestore.getInstance()
        val token=await(FirebaseMessaging.getInstance().token)
        val deviceId=java.security.MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString(""){"%02x".format(it)}
        await(db.collection("members").document(client.uid).collection("devices").document(deviceId).set(mapOf("token" to token,"updatedAt" to FieldValue.serverTimestamp())))
        val manager=compose.activity.getSystemService(NotificationManager::class.java);manager.cancelAll()
        val app=FirebaseApp.initializeApp(compose.activity,FirebaseApp.getInstance().options,"notify-${UUID.randomUUID()}")
        val auth=FirebaseAuth.getInstance(app)
        val tag=System.currentTimeMillis().toString();val id="qa_notice_$tag"
        try {
            val worker=await(auth.createUserWithEmailAndPassword("derook7598+notice$tag@gmail.com","Test!9${UUID.randomUUID()}")).user!!
            val wdb=FirebaseFirestore.getInstance(app)
            await(db.collection("posts").document(id).set(mapOf("author" to client.uid,"title" to "[PRUEBA AVISO] Reparar grifo","description" to "Necesito reparar el grifo de cocina que gotea.","category" to "Plomería","city" to "Armenia","latitude" to 4.535,"longitude" to -75.675,"budget" to 80000L,"imageUri" to null,"status" to "PENDING","assigned" to null,"createdAt" to FieldValue.serverTimestamp(),"updatedAt" to FieldValue.serverTimestamp())))
            await(db.collection("posts").document(id).update(mapOf("status" to "VERIFIED","updatedAt" to FieldValue.serverTimestamp())))
            val offer="${id}_${worker.uid}"
            await(wdb.collection("offers").document(offer).set(mapOf("post" to id,"author" to client.uid,"worker" to worker.uid,"price" to 70000L,"description" to "[PRUEBA] Revisar el empaque.","availability" to "Mañana","createdAt" to FieldValue.serverTimestamp())))
            val sent=notifyAs(worker,"offer",id,offer)
            Assert.assertTrue("FCM result: $sent",sent.optBoolean("push"))
            compose.waitUntil(45_000) { manager.activeNotifications.any { it.id==("offer_$offer").hashCode() } }
            Assert.assertEquals(compose.activity.getString(R.string.notice_offer),manager.activeNotifications.first { it.id==("offer_$offer").hashCode() }.notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
            compose.waitUntil(20_000) { vm.state.value.notices.any { it.post==id } }
            await(db.collection("posts").document(id).update(mapOf("status" to "IN_PROGRESS","assigned" to worker.uid,"updatedAt" to FieldValue.serverTimestamp())))
            val message="message_$tag"
            await(wdb.collection("posts").document(id).collection("messages").document(message).set(mapOf("sender" to worker.uid,"recipient" to client.uid,"text" to "[PRUEBA] Te escribo para coordinar el trabajo.","createdAt" to FieldValue.serverTimestamp())))
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressHome()
            val delivered=notifyAs(worker,"message",id,message)
            Assert.assertTrue("FCM background: $delivered",delivered.optBoolean("push"))
            val noticeId="message_$message"
            compose.waitUntil(45_000) { manager.activeNotifications.any { it.id==noticeId.hashCode() } }
            val notification=manager.activeNotifications.first { it.id==noticeId.hashCode() }.notification
            Assert.assertEquals(compose.activity.getString(R.string.notice_message),notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
            notification.contentIntent.send()
            compose.waitUntil(30_000) { vm.ui.value.page=="chat"&&vm.ui.value.postId==id }
            compose.waitUntil(30_000) { vm.state.value.messages.any { it.text.contains("coordinar") } }
            compose.waitUntil(20_000) { vm.state.value.notices.any { it.id==noticeId&&it.read } }
            compose.waitForIdle();Thread.sleep(400)
            val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            File(compose.activity.filesDir,"aviso-abre-chat.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
            await(db.collection("posts").document(id).update(mapOf("status" to "FINISHED","updatedAt" to FieldValue.serverTimestamp())))
        } finally {
            auth.currentUser?.let { await(it.delete()) };app.delete()
        }
    }
}
