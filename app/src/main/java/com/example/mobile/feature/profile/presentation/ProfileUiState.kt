package com.example.mobile.feature.profile.presentation

import com.example.mobile.core.model.User

data class ProfileUiState(
    val user: User? = null,
    val isLoggingOut: Boolean = false,
    val isUpdatingTheme: Boolean = false,
    val errorMessage: String? = null
)
