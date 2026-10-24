package com.example.servinow.domain.model

data class UserSession(val userId: String, val role: MemberRole)
data class AppPreferences(val session: UserSession? = null, val welcomeSeen: Boolean = false, val rememberSession: Boolean = true)
