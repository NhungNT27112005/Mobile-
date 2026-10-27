package com.example.mobile.feature.auth.presentation

enum class LoginStatus {
    FORM,
    LOADING,
    SUCCESS,
    ERROR
}

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val status: LoginStatus = LoginStatus.FORM,
    val validationMessage: String? = null,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false
) {
    val isLoading: Boolean
        get() = status == LoginStatus.LOADING
}