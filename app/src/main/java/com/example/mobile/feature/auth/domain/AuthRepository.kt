package com.example.mobile.feature.auth.domain

import com.example.mobile.core.model.AuthSession

interface AuthRepository {

    suspend fun login(
        email: String,
        password: String
    ): AuthSession

    suspend fun logout()
}
