package com.example.servinow.domain.model

enum class MemberRole {
    USER,
    MODERATOR,
}

enum class PostStatus(val label: String) {
    PENDING("Pendiente"),
    VERIFIED("Verificada"),
    REJECTED("Rechazada"),
    IN_PROGRESS("En progreso"),
    FINISHED("Finalizada"),
}

data class Member(
    val id: String,
    val name: String,
    val email: String,
    val role: MemberRole = MemberRole.USER,
    val phone: String = "",
    val city: String = "Bogotá",
    val latitude: Double = 4.6486,
    val longitude: Double = -74.0628,
    val profession: String = "",
    val bio: String = "",
    val experience: String = "",
    val training: String = "",
    val skills: String = "",
    val years: Int = 0,
)

data class WorkPost(
    val id: String,
    val author: String,
    val title: String,
    val category: String,
    val description: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val budget: Long,
    val hasImage: Boolean = true,
    val status: PostStatus = PostStatus.PENDING,
    val reason: String = "",
    val assigned: String? = null,
    val interested: Set<String> = emptySet(),
    val createdAt: Long = System.currentTimeMillis(),
)

data class WorkOffer(
    val id: String,
    val post: String,
    val worker: String,
    val price: Long,
    val description: String,
    val availability: String,
)

data class WorkComment(val id: String, val post: String, val author: String, val text: String)

data class WorkerReview(
    val id: String,
    val post: String,
    val worker: String,
    val author: String,
    val stars: Int,
    val text: String,
)

data class MemberNotice(
    val id: String,
    val recipient: String,
    val title: String,
    val body: String,
    val post: String? = null,
    val read: Boolean = false,
)

data class CommunityState(
    val users: List<Member> = emptyList(),
    val posts: List<WorkPost> = emptyList(),
    val offers: List<WorkOffer> = emptyList(),
    val comments: List<WorkComment> = emptyList(),
    val reviews: List<WorkerReview> = emptyList(),
    val notices: List<MemberNotice> = emptyList(),
    val points: Map<String, Int> = emptyMap(),
    val rewards: Set<String> = emptySet(),
    val currentUser: String? = null,
) {
    val me: Member?
        get() = users.find { it.id == currentUser }
}
