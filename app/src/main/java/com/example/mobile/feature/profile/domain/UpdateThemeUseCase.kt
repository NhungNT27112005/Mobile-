package com.example.mobile.feature.profile.domain

class UpdateThemeUseCase(
    private val repository: SettingsRepository
) {

    suspend operator fun invoke(isDarkTheme: Boolean) {
        repository.setDarkTheme(isDarkTheme)
    }
}
