package com.example.mobile.core.model

data class AuthSession(
    val accessToken: String,
    val expiresAt: Long,
    val user: User
)
