package com.example.mobile.feature.auth.domain

import android.util.Patterns
import com.example.mobile.core.model.AuthSession

class LoginValidationException(message: String) : IllegalArgumentException(message)

class LoginUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String
    ): AuthSession {
        val cleanEmail = email.trim()

        if (cleanEmail.isBlank() && password.isBlank()) {
            throw LoginValidationException("Vui lòng nhập email và mật khẩu")
        }

        if (cleanEmail.isBlank()) {
            throw LoginValidationException("Vui lòng nhập email")
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            throw LoginValidationException("Email không hợp lệ")
        }

        if (password.isBlank()) {
            throw LoginValidationException("Vui lòng nhập mật khẩu")
        }

        if (password.length < 8) {
            throw LoginValidationException("Mật khẩu cần ít nhất 8 ký tự")
        }

        if (password.length > 128) {
            throw LoginValidationException("Mật khẩu tối đa 128 ký tự")
        }

        return repository.login(
            email = cleanEmail,
            password = password
        )
    }
}
