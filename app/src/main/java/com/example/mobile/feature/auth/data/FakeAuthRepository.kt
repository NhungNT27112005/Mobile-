package com.example.mobile.feature.auth.data

import com.example.mobile.core.model.AuthSession
import com.example.mobile.core.model.User
import com.example.mobile.feature.auth.domain.AuthException
import com.example.mobile.feature.auth.domain.AuthRepository
import kotlinx.coroutines.delay

class FakeAuthRepository : AuthRepository {

    override suspend fun login(
        email: String,
        password: String
    ): AuthSession {
        delay(800)

        if (email != "nhung@example.com" || password != "NewsHub123") {
            throw AuthException("Email hoặc mật khẩu không đúng.")
        }

        return AuthSession(
            accessToken = "demo-access-token",
            expiresAt = System.currentTimeMillis() / 1000 + 86400,
            user = User(
                id = "demo-user",
                displayName = "Nhung",
                email = "nhung@example.com"
            )
        )
    }

    override suspend fun logout() {
        delay(300)
    }
}
