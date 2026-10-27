package com.example.mobile.feature.auth.domain

class AuthException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
