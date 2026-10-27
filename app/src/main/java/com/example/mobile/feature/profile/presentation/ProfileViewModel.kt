package com.example.mobile.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile.core.session.SessionManager
import com.example.mobile.feature.auth.domain.AuthException
import com.example.mobile.feature.auth.domain.LogoutUseCase
import com.example.mobile.feature.profile.domain.UpdateThemeUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val logoutUseCase: LogoutUseCase,
    private val updateThemeUseCase: UpdateThemeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            user = SessionManager.session.value?.user
        )
    )
    val uiState = _uiState.asStateFlow()

    fun updateTheme(isDarkTheme: Boolean) {
        val currentState = _uiState.value

        if (currentState.isUpdatingTheme || currentState.isLoggingOut) return

        _uiState.update {
            it.copy(
                isUpdatingTheme = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                updateThemeUseCase(isDarkTheme)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage =
                            "Không thể lưu giao diện. Vui lòng thử lại."
                    )
                }
            } finally {
                _uiState.update {
                    it.copy(isUpdatingTheme = false)
                }
            }
        }
    }

    fun logout() {
        if (
            _uiState.value.isLoggingOut ||
            _uiState.value.isUpdatingTheme
        ) return

        _uiState.update {
            it.copy(
                isLoggingOut = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                logoutUseCase()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                _uiState.update {
                    it.copy(errorMessage = e.message)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage =
                            "Không thể đăng xuất. Vui lòng thử lại."
                    )
                }
            } finally {
                _uiState.update {
                    it.copy(isLoggingOut = false)
                }
            }
        }
    }
}
