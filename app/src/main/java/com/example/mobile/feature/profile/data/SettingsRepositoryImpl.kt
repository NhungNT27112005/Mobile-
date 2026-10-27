package com.example.mobile.feature.profile.data

import com.example.mobile.core.datastore.SettingsDataStore
import com.example.mobile.feature.profile.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override val isDarkTheme: Flow<Boolean> =
        settingsDataStore.isDarkTheme

    override suspend fun setDarkTheme(enabled: Boolean) {
        settingsDataStore.setDarkTheme(enabled)
    }
}
