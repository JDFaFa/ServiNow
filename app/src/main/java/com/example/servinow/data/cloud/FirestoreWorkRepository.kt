package com.example.servinow.data.cloud

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import com.example.servinow.domain.model.WorkPost
import com.example.servinow.domain.model.PostStatus
import com.example.servinow.domain.model.PrivateMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Remote data only. Demo identities are never promoted to authenticated Firebase users. */
@Singleton
class FirestoreWorkRepository @Inject constructor() {
    private val db get() = FirebaseFirestore.getInstance()
    private val uid: String get() = FirebaseAuth.getInstance().currentUser?.uid ?: error("SIGN_IN_REQUIRED")
    private fun post(id: String) = db.collection("posts").document(id.also { require(it.isNotBlank() && '/' !in it) })

    fun publicPosts(): Flow<List<WorkPost>> = callbackFlow {
        val listener = db.collection("posts").whereIn("status", listOf("VERIFIED", "IN_PROGRESS", "FINISHED")).limit(100)
            .addSnapshotListener { data, error ->
                if (error != null) close(error)
                else trySend(data?.documents.orEmpty().mapNotNull(::decodePost))
            }
        awaitClose { listener.remove() }
    }

    suspend fun save(value: WorkPost) {
        val actor = uid
        require(value.author == actor)
        require(value.title.trim().length in 5..120 && value.description.trim().length in 10..3000 && value.budget > 0)
        require(value.imageUri == null || value.imageUri.startsWith("https://res.cloudinary.com/"))
        val ref = post(value.id)
        db.runTransaction { tx ->
            val old = tx.get(ref)
            if (old.exists()) {
                check(old.getString("author") == actor && old.getString("status") in listOf("PENDING","REJECTED","VERIFIED"))
                tx.update(ref, mapOf("title" to value.title.trim(), "description" to value.description.trim(), "category" to value.category,
                    "city" to value.city, "latitude" to value.latitude, "longitude" to value.longitude, "budget" to value.budget,
                    "imageUri" to value.imageUri, "status" to "PENDING", "updatedAt" to FieldValue.serverTimestamp()))
            } else tx.set(ref, mapOf("author" to actor, "title" to value.title.trim(), "description" to value.description.trim(),
                "category" to value.category, "city" to value.city, "latitude" to value.latitude, "longitude" to value.longitude,
                "budget" to value.budget, "imageUri" to value.imageUri, "status" to "PENDING", "assigned" to null,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()))
        }.waitResult()
    }

    suspend fun offer(postId: String, price: Long, description: String, availability: String) {
        val actor = uid
        require(price > 0 && description.trim().length in 5..1000 && availability.trim().length in 1..200)
        post(postId).collection("offers").document(actor).set(mapOf("worker" to actor,"price" to price,
            "description" to description.trim(),"availability" to availability.trim(),"createdAt" to FieldValue.serverTimestamp())).waitResult()
    }

    suspend fun accept(postId: String, workerId: String) {
        val actor = uid; val ref = post(postId)
        db.runTransaction { tx ->
            val value = tx.get(ref); val offer = tx.get(ref.collection("offers").document(workerId))
            check(value.getString("author") == actor && value.getString("status") == "VERIFIED" && offer.exists())
            tx.update(ref, mapOf("assigned" to workerId,"status" to "IN_PROGRESS","updatedAt" to FieldValue.serverTimestamp()))
        }.waitResult()
    }

    suspend fun finish(postId: String) {
        val actor = uid; val ref = post(postId)
        db.runTransaction { tx ->
            val value = tx.get(ref)
            check(value.getString("author") == actor && value.getString("status") == "IN_PROGRESS")
            tx.update(ref,mapOf("status" to "FINISHED","updatedAt" to FieldValue.serverTimestamp()))
        }.waitResult()
    }

    fun messages(postId: String): Flow<List<PrivateMessage>> = callbackFlow {
        uid // Refuse to start a private listener without a session.
        val listener = post(postId).collection("messages").orderBy("createdAt", Query.Direction.DESCENDING).limit(100)
            .addSnapshotListener { data, error ->
                if (error != null) close(error)
                else trySend(data?.documents.orEmpty().map { d -> PrivateMessage(d.id,postId,d.getString("sender").orEmpty(),
                    d.getString("recipient").orEmpty(),d.getString("text").orEmpty(),d.getTimestamp("createdAt")?.toDate()?.time ?: 0) }.reversed())
            }
        awaitClose { listener.remove() }
    }

    suspend fun send(postId: String, text: String): String {
        val actor = uid; require(text.trim().length in 1..2000)
        val ref = post(postId); val message = ref.collection("messages").document()
        db.runTransaction { tx ->
            val p = tx.get(ref); val author = p.getString("author"); val worker = p.getString("assigned")
            check(worker != null && actor in setOf(author,worker))
            tx.set(message,mapOf("sender" to actor,"recipient" to (if(actor == author) worker else author),
                "text" to text.trim(),"createdAt" to FieldValue.serverTimestamp()))
        }.waitResult()
        return message.id
    }

    suspend fun saveDevice(token: String) {
        val actor = uid
        val id = java.security.MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
        db.collection("members").document(actor).collection("devices").document(id)
            .set(mapOf("token" to token, "updatedAt" to FieldValue.serverTimestamp())).waitResult()
    }

    private fun decodePost(d: DocumentSnapshot): WorkPost? = runCatching {
        WorkPost(d.id,d.getString("author")!!,d.getString("title")!!,d.getString("category")!!,
            d.getString("description")!!,d.getString("city")!!,d.getDouble("latitude")!!,d.getDouble("longitude")!!,
            d.getLong("budget")!!,imageUri = d.getString("imageUri"),hasImage = d.getString("imageUri") != null,
            status = PostStatus.valueOf(d.getString("status")!!),assigned = d.getString("assigned"),
            createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: 0)
    }.getOrNull()
}
