package com.example.servinow.domain.model
data class Professional(val id: String, val name: String, val trade: String, val years: Int)
data class Proposal(val id: String, val job: String, val title: String, val worker: String,
    val price: Long, val message: String, val availability: String, val duration: String, val mine: Boolean = false)
data class ServiceReview(val job: String, val worker: String, val stars: Int, val text: String)
data class ServiceData(val offers: List<Proposal> = emptyList(), val accepted: Map<String,String> = emptyMap(),
    val completed: Set<String> = emptySet(), val doneRequests: Set<String> = emptySet(),
    val reviews: List<ServiceReview> = emptyList())
