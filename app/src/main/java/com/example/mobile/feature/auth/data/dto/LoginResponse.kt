package com.example.mobile.feature.auth.data.dto

import com.example.mobile.core.model.User

data class LoginResponse(
    val accessToken: String,
    val expiresAt: Long,
    val user: User
)
