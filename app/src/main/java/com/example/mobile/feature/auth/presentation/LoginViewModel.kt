package com.example.mobile.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile.core.session.SessionManager
import com.example.mobile.feature.auth.domain.AuthException
import com.example.mobile.feature.auth.domain.LoginUseCase
import com.example.mobile.feature.auth.domain.LoginValidationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isLoggedIn = SessionManager.session.value?.let {
                it.expiresAt > System.currentTimeMillis() / 1000
            } ?: false
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        if (_uiState.value.status != LoginStatus.FORM) return

        _uiState.update {
            it.copy(email = email, validationMessage = null)
        }
    }

    fun onPasswordChanged(password: String) {
        if (_uiState.value.status != LoginStatus.FORM) return

        _uiState.update {
            it.copy(password = password, validationMessage = null)
        }
    }

    fun login() {
        val currentState = _uiState.value
        if (currentState.status != LoginStatus.FORM || currentState.isLoggedIn) return

        _uiState.update {
            it.copy(
                status = LoginStatus.LOADING,
                validationMessage = null,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val session = loginUseCase(
                    email = currentState.email,
                    password = currentState.password
                )

                try {
                    SessionManager.update(session)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    throw AuthException("Không thể lưu phiên đăng nhập. Vui lòng thử lại.", e)
                }

                _uiState.update {
                    it.copy(
                        password = "",
                        status = LoginStatus.SUCCESS,
                        isLoggedIn = true
                    )
                }
            } catch (e: CancellationException) {
                _uiState.update { it.copy(status = LoginStatus.FORM) }
                throw e
            } catch (e: LoginValidationException) {
                _uiState.update {
                    it.copy(
                        status = LoginStatus.FORM,
                        validationMessage = e.message
                    )
                }
            } catch (e: AuthException) {
                _uiState.update {
                    it.copy(
                        password = "",
                        status = LoginStatus.ERROR,
                        errorMessage = e.message
                            ?: "Không thể đăng nhập lúc này. Vui lòng thử lại."
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        password = "",
                        status = LoginStatus.ERROR,
                        errorMessage = "Không thể đăng nhập lúc này. Vui lòng thử lại."
                    )
                }
            }
        }
    }

    fun retry() {
        if (_uiState.value.status != LoginStatus.ERROR) return

        _uiState.update {
            it.copy(
                status = LoginStatus.FORM,
                validationMessage = null,
                errorMessage = null
            )
        }
    }

    fun continueToHome() {
        if (_uiState.value.status != LoginStatus.SUCCESS) return
        _uiState.update { it.copy(isLoggedIn = true) }
    }
}
