package com.example.servinow.data.cloud

import com.example.servinow.domain.model.*
import com.example.servinow.domain.repository.RemoteCommunityRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreCommunityRepository @Inject constructor(private val jobs: FirestoreWorkRepository, private val gateway: CloudGateway) : RemoteCommunityRepository {
    private val db get() = FirebaseFirestore.getInstance()
    private val actor get() = FirebaseAuth.getInstance().currentUser?.uid ?: error("SIGN_IN_REQUIRED")
    private val mutable = MutableStateFlow(CommunityState())
    override val state = mutable.asStateFlow()
    private val failures = MutableStateFlow<String?>(null)
    override val error = failures.asStateFlow()
    private val listeners = mutableListOf<ListenerRegistration>()
    private val detailListeners = mutableListOf<ListenerRegistration>()
    private val postGroups = mutableMapOf<String,List<WorkPost>>()
    private val offerGroups = mutableMapOf<String,List<WorkOffer>>()
    private var generation = 0
    private var selected: String? = null
    private var selectedListener: ListenerRegistration? = null
    private var selectedAccess: Pair<PostStatus, String?>? = null
    private fun post(id: String) = db.collection("posts").document(id)
    private fun Query.watch(bucket: MutableList<ListenerRegistration> = listeners, update: (QuerySnapshot) -> Unit) {
        val epoch = generation
        bucket += addSnapshotListener { result, error ->
            if (epoch == generation) {
                if (error != null) failures.value = "CLOUD_LOAD_FAILED"
                else if (result != null) { update(result) }
            }
        }
    }
    override suspend fun connect(member: Member) {
        disconnect()
        check(actor == member.id)
        mutable.value = CommunityState(users = listOf(member), currentUser = member.id)
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener(ServiNowMessagingService::registerToken)
        val profile = db.collection("profiles").document(member.id)
        db.runTransaction { tx ->
            if (!tx.get(profile).exists()) tx.set(profile, profileData(member))
        }.waitResult()
        val epoch = generation
        listeners += profile.addSnapshotListener { d, e ->
            if (epoch == generation) {
                if(e != null) failures.value="CLOUD_LOAD_FAILED"
                else if(d != null && d.exists()) mutable.update { s ->
                    val previous=s.me ?: member
                    val own=decodeMember(d).copy(email=member.email, role=member.role, phone=previous.phone, latitude=previous.latitude, longitude=previous.longitude)
                    s.copy(users=s.users.filterNot { it.id==own.id }+own)
                }
            }
        }
        listeners += db.collection("members").document(member.id).addSnapshotListener { d,e ->
            if(epoch==generation && e==null && d!=null && d.exists()) mutable.update { s ->
                s.copy(users=s.users.map { if(it.id==member.id) it.copy(phone=d.getString("phone").orEmpty(),latitude=d.getDouble("latitude")?:it.latitude,longitude=d.getDouble("longitude")?:it.longitude) else it })
            }
        }
        db.collection("profiles").limit(100).watch { q -> mutable.update { s ->
            val own=s.me
            s.copy(users=q.documents.map(::decodeMember).filterNot { it.id==own?.id }+listOfNotNull(own))
        } }
        fun posts(key:String, query:Query) { query.watch { q ->
            postGroups[key]=q.documents.mapNotNull(::decodePost)
            mutable.update { it.copy(posts=postGroups.values.flatten().distinctBy { p->p.id }.sortedByDescending { p->p.createdAt }) }
        } }
        posts("public", db.collection("posts").whereIn("status",listOf("VERIFIED","IN_PROGRESS","FINISHED")).limit(100))
        posts("own", db.collection("posts").whereEqualTo("author",member.id).limit(100))
        if(member.role==MemberRole.MODERATOR) posts("pending",db.collection("posts").whereEqualTo("status","PENDING").limit(100))
        fun offers(key:String, field:String) { db.collection("offers").whereEqualTo(field,member.id).limit(100).watch { q ->
            offerGroups[key]=q.documents.map { d->WorkOffer(d.id,d.getString("post").orEmpty(),d.getString("worker").orEmpty(),d.getLong("price")?:0,d.getString("description").orEmpty(),d.getString("availability").orEmpty()) }
            mutable.update { it.copy(offers=offerGroups.values.flatten().distinctBy { o->o.id }) }
        } }
        offers("sent","worker"); offers("received","author")
        db.collection("members").document(member.id).collection("rewards").watch { q ->
            val keys=q.documents.mapNotNull { it.getString("key") }.map { "${member.id}:$it" }.toMutableSet()
            if(keys.any { it.startsWith("${member.id}:create:") })keys += "${member.id}:badge:first"
            if(keys.count { it.startsWith("${member.id}:comment:") }>=10)keys += "${member.id}:badge:comments"
            if(keys.count { it.startsWith("${member.id}:verify:") }>=10)keys += "${member.id}:badge:verified"
            mutable.update { it.copy(points=mapOf(member.id to q.documents.sumOf { d -> (d.getLong("amount")?:0).toInt() }),rewards=keys) }
        }
        db.collection("reviews").limit(100).watch { q -> mutable.update { it.copy(reviews=q.documents.map { d->WorkerReview(d.id,d.getString("post").orEmpty(),d.getString("worker").orEmpty(),d.getString("author").orEmpty(),(d.getLong("stars")?:0).toInt(),d.getString("text").orEmpty()) }) } }
        db.collection("members").document(member.id).collection("notices").orderBy("createdAt",Query.Direction.DESCENDING).limit(100).watch { q -> mutable.update { it.copy(notices=q.documents.map { d->MemberNotice(d.id,member.id,d.getString("title").orEmpty(),d.getString("body").orEmpty(),d.getString("post"),d.getBoolean("read")?:false) }) } }
    }
    override fun disconnect() {
        generation++; listeners.forEach { it.remove() }; listeners.clear()
        detailListeners.forEach { it.remove() }; detailListeners.clear()
        selectedListener?.remove(); selectedListener=null; selectedAccess=null
        postGroups.clear(); offerGroups.clear(); selected=null
        mutable.value=CommunityState(); failures.value=null
    }
    override fun observePost(id: String) {
        selectedListener?.remove()
        selected=id; selectedAccess=null
        detailListeners.forEach { it.remove() }; detailListeners.clear()
        mutable.update { it.copy(comments=emptyList(),messages=emptyList()) }
        val epoch=generation
        // Listen to the job itself: acceptance can arrive after this screen opens.
        selectedListener=post(id).addSnapshotListener { document,error ->
            if(epoch!=generation || selected!=id) return@addSnapshotListener
            if(error!=null) { failures.value="CLOUD_LOAD_FAILED"; return@addSnapshotListener }
            val p=document?.let(::decodePost) ?: return@addSnapshotListener
            mutable.update { state ->
                val previous=state.posts.find { it.id==id }
                state.copy(posts=state.posts.filterNot { it.id==id }+p.copy(interested=previous?.interested.orEmpty()))
            }
            val access=p.status to p.assigned
            if(selectedAccess!=access) {
                selectedAccess=access
                detailListeners.forEach { it.remove() }; detailListeners.clear()
                mutable.update { it.copy(comments=emptyList(),messages=emptyList()) }
                observePostContent(id,p)
            }
        }
    }
    private fun observePostContent(id:String,p:WorkPost) {
        if(p.status in setOf(PostStatus.VERIFIED,PostStatus.IN_PROGRESS,PostStatus.FINISHED)) post(id).collection("comments").orderBy("createdAt",Query.Direction.DESCENDING).limit(100).watch(detailListeners) { q ->
            if(selected==id) mutable.update { it.copy(comments=q.documents.map { d->WorkComment(d.id,id,d.getString("author").orEmpty(),d.getString("text").orEmpty()) }.reversed()) }
        }
        if(p.status in setOf(PostStatus.VERIFIED,PostStatus.IN_PROGRESS,PostStatus.FINISHED)) post(id).collection("votes").limit(1000).watch(detailListeners) { q ->
            if(selected==id) mutable.update { s -> s.copy(posts=s.posts.map { if(it.id==id) it.copy(interested=q.documents.map { d->d.id }.toSet()) else it }) }
        }
        if(p.assigned!=null && actor in setOf(p.author,p.assigned)) {
            post(id).collection("messages").orderBy("createdAt",Query.Direction.DESCENDING).limit(100).watch(detailListeners) { q ->
                if(selected==id) mutable.update { it.copy(messages=q.documents.map { d->PrivateMessage(d.id,id,d.getString("sender").orEmpty(),d.getString("recipient").orEmpty(),d.getString("text").orEmpty(),d.getTimestamp("createdAt")?.toDate()?.time?:0) }.reversed()) }
            }
        }
    }
    private suspend fun reward(type:String,id:String,event:String) {
        try { gateway.recordActivity(type,id,event) }
        catch(e:kotlinx.coroutines.CancellationException) { throw e }
        catch(_:Exception) { failures.value="CLOUD_LOAD_FAILED" }
    }
    override suspend fun savePost(post: WorkPost) {
        val uri = post.imageUri
        val uploaded = if (uri != null && !uri.startsWith("https://res.cloudinary.com/")) gateway.upload(uri) else uri
        jobs.save(post.copy(imageUri = uploaded, hasImage = uploaded != null))
        reward("create",post.id,post.id)
    }
    private suspend fun announce(type:String,id:String,event:String) {
        try { gateway.notifyEvent(type,id,event) }
        catch(e:kotlinx.coroutines.CancellationException) { throw e }
        catch(_:Exception) { failures.value="CLOUD_LOAD_FAILED" }
    }
    override suspend fun finish(id: String) { jobs.finish(id); announce("finished",id,id) }
    override suspend fun sendMessage(id: String,text:String) { val event=jobs.send(id,text); announce("message",id,event) }
    override suspend fun deletePost(id:String) { post(id).update(mapOf("status" to "DELETED","updatedAt" to FieldValue.serverTimestamp())).waitResult() }
    override suspend fun verify(id:String) { post(id).update(mapOf("status" to "VERIFIED","updatedAt" to FieldValue.serverTimestamp())).waitResult(); reward("verify",id,id); announce("approved",id,id) }
    override suspend fun reject(id:String,reason:String) {
        require(reason.trim().length in 5..500)
        post(id).update(mapOf("status" to "REJECTED","reason" to reason.trim(),"updatedAt" to FieldValue.serverTimestamp())).waitResult()
        announce("rejected",id,id)
    }
    override suspend fun vote(id:String) {
        val ref=post(id).collection("votes").document(actor)
        db.runTransaction { tx -> if(tx.get(ref).exists())tx.delete(ref) else tx.set(ref,mapOf("author" to actor,"createdAt" to FieldValue.serverTimestamp())) }.waitResult()
    }
    override suspend fun comment(id:String,text:String) {
        require(text.trim().length in 1..1000)
        val comment=post(id).collection("comments").add(mapOf("author" to actor,"text" to text.trim(),"createdAt" to FieldValue.serverTimestamp())).waitResult()
        reward("comment",id,comment.id)
        announce("comment",id,comment.id)
    }
    override suspend fun offer(id:String,price:String,text:String,availability:String) {
        val amount=price.toLongOrNull();require(amount!=null&&amount>0&&text.trim().length in 5..1000&&availability.trim().length in 1..200)
        val ref=db.collection("offers").document("${id}_${actor}")
        db.runTransaction { tx ->
            val p=tx.get(post(id));check(!tx.get(ref).exists()) { "Ya enviaste una oferta." }
            check(p.getString("status")=="VERIFIED"&&p.getString("author")!=actor)
            tx.set(ref,mapOf("post" to id,"author" to p.getString("author"),"worker" to actor,"price" to amount,"description" to text.trim(),"availability" to availability.trim(),"createdAt" to FieldValue.serverTimestamp()))
        }.waitResult()
        announce("offer",id,ref.id)
    }
    override suspend fun accept(offerId:String) {
        var acceptedPost=""
        val offer=db.collection("offers").document(offerId)
        db.runTransaction { tx ->
            val o=tx.get(offer);val ref=post(o.getString("post")?:error("Oferta no disponible."));val p=tx.get(ref)
            check(p.getString("author")==actor && p.getString("status")=="VERIFIED")
            acceptedPost=ref.id
            tx.update(ref,mapOf("assigned" to o.getString("worker"),"status" to "IN_PROGRESS","updatedAt" to FieldValue.serverTimestamp()))
        }.waitResult()
        announce("accepted",acceptedPost,acceptedPost)
        // Keep the selected job listener alive while acceptance propagates.
    }
    override suspend fun review(id:String,stars:Int,text:String) {
        require(stars in 1..5 && text.trim().length in 1..1000)
        val ref=db.collection("reviews").document(id)
        db.runTransaction { tx ->
            val p=tx.get(post(id));check(!tx.get(ref).exists());check(p.getString("author")==actor && p.getString("status")=="FINISHED")
            tx.set(ref,mapOf("post" to id,"author" to actor,"worker" to p.getString("assigned"),"stars" to stars,"text" to text.trim(),"createdAt" to FieldValue.serverTimestamp()))
        }.waitResult()
    }
    override suspend fun updateMember(member:Member) {
        check(member.id==actor);require(member.name.trim().length in 2..100)
        require(member.email==FirebaseAuth.getInstance().currentUser?.email) { "El cambio de correo requiere verificación." }
        val uri = member.photoUri
        val uploaded = if (uri != null && !uri.startsWith("https://res.cloudinary.com/")) gateway.upload(uri) else uri
        db.batch().set(db.collection("profiles").document(actor),profileData(member.copy(photoUri = uploaded)))
            .set(db.collection("members").document(actor),mapOf("phone" to member.phone,"latitude" to member.latitude,"longitude" to member.longitude)).commit().waitResult()
    }
    override suspend fun markNotice(id:String,read:Boolean) { db.collection("members").document(actor).collection("notices").document(id).update("read",read).waitResult() }
    override suspend fun markAllNotices() {
        val batch=db.batch();state.value.notices.filterNot { it.read }.forEach { batch.update(db.collection("members").document(actor).collection("notices").document(it.id),"read",true) };batch.commit().waitResult()
    }
    private fun profileData(m:Member)=mapOf("name" to m.name.trim(),"city" to m.city,"profession" to m.profession,"bio" to m.bio,"experience" to m.experience,"training" to m.training,"skills" to m.skills,"years" to m.years,"photoUri" to m.photoUri)
    private fun decodeMember(d:DocumentSnapshot)=Member(d.id,d.getString("name").orEmpty(),"",city=d.getString("city").orEmpty(),profession=d.getString("profession").orEmpty(),bio=d.getString("bio").orEmpty(),experience=d.getString("experience").orEmpty(),training=d.getString("training").orEmpty(),skills=d.getString("skills").orEmpty(),years=(d.getLong("years")?:0).toInt(),photoUri=d.getString("photoUri"))
    private fun decodePost(d:DocumentSnapshot):WorkPost? = runCatching {
        WorkPost(d.id,d.getString("author")!!,d.getString("title")!!,d.getString("category")!!,d.getString("description")!!,d.getString("city")!!,d.getDouble("latitude")!!,d.getDouble("longitude")!!,d.getLong("budget")!!,hasImage=d.getString("imageUri")!=null,imageUri=d.getString("imageUri"),status=PostStatus.valueOf(d.getString("status")!!),reason=d.getString("reason").orEmpty(),assigned=d.getString("assigned"),createdAt=d.getTimestamp("createdAt")?.toDate()?.time?:0)
    }.getOrNull()
}
