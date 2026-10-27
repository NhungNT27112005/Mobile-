package com.example.mobile.feature.profile.domain

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val isDarkTheme: Flow<Boolean>

    suspend fun setDarkTheme(enabled: Boolean)
}
