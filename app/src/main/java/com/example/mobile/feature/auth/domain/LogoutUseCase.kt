package com.example.mobile.feature.auth.domain

import com.example.mobile.core.session.SessionManager
import java.io.IOException

class LogoutUseCase(
    private val repository: AuthRepository
) {

    suspend operator fun invoke() {
        repository.logout()

        try {
            SessionManager.clear()
        } catch (e: IOException) {
            throw AuthException(
                "Không thể xóa phiên đăng nhập. Vui lòng thử lại.",
                e
            )
        }
    }
}
