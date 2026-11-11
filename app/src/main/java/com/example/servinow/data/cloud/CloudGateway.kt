package com.example.servinow.data.cloud

import android.content.Context
import android.net.Uri
import com.example.servinow.R
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun <T> Task<T>.waitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}

/** Only a public backend URL is in Android resources; credentials remain on the server. */
@Singleton
class CloudGateway @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun isModerator(): Boolean = request("bootstrap", "{}".toByteArray(), "application/json").optBoolean("moderator",false)
    suspend fun deleteAccount(confirmation: String) {
        request("account-delete", JSONObject().put("confirmation",confirmation).toString().toByteArray(), "application/json")
    }
    suspend fun recordActivity(type: String, post: String, event: String) {
        request("activity", JSONObject().put("type",type).put("post",post).put("event",event).toString().toByteArray(), "application/json")
    }
    suspend fun notifyEvent(type: String, post: String, event: String) {
        request("notify", JSONObject().put("type",type).put("post",post).put("event",event).toString().toByteArray(), "application/json")
    }
    suspend fun improve(text: String, language: String): String = request("description",
        JSONObject().put("text", text).put("language", language).toString().toByteArray(), "application/json").getString("description")

    suspend fun upload(uri: String): String = withContext(Dispatchers.IO) {
        // Bounded read, independent of the ContentProvider's reported size.
        val bytes = context.contentResolver.openInputStream(Uri.parse(uri))?.use { source ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) { val count = source.read(buffer); if (count < 0) break
                require(output.size() + count <= 5 * 1024 * 1024) { "IMAGE_SIZE" }; output.write(buffer, 0, count)
            }; output.toByteArray()
        } ?: error("IMAGE_UNREADABLE")
        request("images", bytes, "application/octet-stream").getString("url")
    }

    private suspend fun request(path: String, bytes: ByteArray, type: String): JSONObject {
        val root = context.getString(R.string.services_base_url).trimEnd('/')
        check(root.startsWith("https://")) { "SERVICE_NOT_CONFIGURED" }
        val user = FirebaseAuth.getInstance().currentUser ?: error("SIGN_IN_REQUIRED")
        val token = user.getIdToken(false).waitResult().token ?: error("SIGN_IN_REQUIRED")
        return withContext(Dispatchers.IO) {
            val connection = URL("$root/v1/$path").openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"; connection.connectTimeout = 15000; connection.readTimeout = if (path == "description") 45000 else 30000
                connection.instanceFollowRedirects = false; connection.doOutput = true
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Content-Type", type)
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val raw = stream?.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(4096)
                    while (true) { val count = input.read(buffer); if (count < 0) break
                        check(output.size() + count <= 65536) { "SERVICE_ERROR" }
                        output.write(buffer, 0, count)
                    }
                    output.toString("UTF-8")
                }.orEmpty()
                val body = runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
                check(status in 200..299) { body.optString("error", "SERVICE_ERROR") }
                body
            } finally { connection.disconnect() }
        }
    }
}
