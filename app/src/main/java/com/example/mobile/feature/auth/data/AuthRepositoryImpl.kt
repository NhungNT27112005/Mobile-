package com.example.mobile.feature.auth.data

import com.example.mobile.core.model.AuthSession
import com.example.mobile.core.model.User
import com.example.mobile.feature.auth.data.dto.LoginRequest
import com.example.mobile.feature.auth.domain.AuthException
import com.example.mobile.feature.auth.domain.AuthRepository
import com.google.gson.JsonParseException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

class AuthRepositoryImpl(
    private val apiService: AuthApiService
) : AuthRepository {

    override suspend fun login(
        email: String,
        password: String
    ): AuthSession {
        try {
            val response = apiService.login(
                LoginRequest(
                    email = email,
                    password = password
                )
            )

            // Gson can return null for missing JSON fields, even with non-null Kotlin types.
            val accessToken: String? = response.accessToken
            val user: User? = response.user
            val now = System.currentTimeMillis() / 1000

            if (
                accessToken.isNullOrBlank() ||
                response.expiresAt <= now ||
                user == null ||
                user.id.isNullOrBlank() ||
                user.displayName.isNullOrBlank() ||
                user.email.isNullOrBlank()
            ) {
                throw AuthException("Dữ liệu đăng nhập không hợp lệ.")
            }

            return AuthSession(
                accessToken = accessToken,
                expiresAt = response.expiresAt,
                user = user
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            val message = when (e.code()) {
                400, 422 -> "Thông tin đăng nhập không hợp lệ."
                401 -> "Email hoặc mật khẩu không đúng."
                429 -> "Bạn đã thử quá nhiều lần. Vui lòng thử lại sau."
                in 500..599 -> "Máy chủ đang gặp lỗi. Vui lòng thử lại sau."
                else -> "Không thể đăng nhập lúc này. Vui lòng thử lại."
            }
            throw AuthException(message, e)
        } catch (e: IOException) {
            throw AuthException(
                "Không thể kết nối máy chủ. Vui lòng kiểm tra mạng và thử lại.",
                e
            )
        } catch (e: JsonParseException) {
            throw AuthException("Dữ liệu đăng nhập không hợp lệ.", e)
        }
    }

    override suspend fun logout() {
        try {
            apiService.logout()
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            // The session is already expired or invalid; allow local cleanup.
            if (e.code() == 401) return

            val message = when (e.code()) {
                429 -> "Bạn thao tác quá nhanh. Vui lòng thử lại sau."
                in 500..599 -> "Máy chủ đang gặp lỗi. Vui lòng thử lại."
                else -> "Không thể đăng xuất. Vui lòng thử lại."
            }

            throw AuthException(message, e)
        } catch (e: IOException) {
            throw AuthException(
                "Không thể kết nối máy chủ. Vui lòng kiểm tra mạng và thử lại.",
                e
            )
        }
    }
}
