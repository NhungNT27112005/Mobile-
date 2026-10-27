
package com.example.mobile.feature.auth.data

import com.example.mobile.feature.auth.data.dto.LoginRequest
import com.example.mobile.feature.auth.data.dto.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @POST("auth/logout")
    suspend fun logout()
}
