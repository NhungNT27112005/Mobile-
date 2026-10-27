package com.example.mobile.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile.feature.home.domain.GetCategoriesUseCase
import com.example.mobile.feature.home.domain.GetNewsUseCase
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

class HomeViewModel(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getNewsUseCase: GetNewsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadHome()
    }

    fun onCategorySelected(categoryId: String?) {
        if (categoryId == _uiState.value.selectedCategoryId) return

        _uiState.update {
            it.copy(selectedCategoryId = categoryId)
        }

        loadHome()
    }

    fun retry() {
        if (_uiState.value.isLoading) return

        loadHome()
    }

    private fun loadHome() {
        val categoryId = _uiState.value.selectedCategoryId

        loadJob?.cancel()

        _uiState.update {
            it.copy(
                isLoading = true,
                articles = emptyList(),
                errorMessage = null
            )
        }

        loadJob = viewModelScope.launch {
            try {
                if (_uiState.value.categories.isEmpty()) {
                    val categories = getCategoriesUseCase()

                    _uiState.update {
                        it.copy(categories = categories)
                    }
                }

                val articles = getNewsUseCase(categoryId)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        articles = articles
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = when (e) {
                    is HttpException -> when (e.code()) {
                        401 -> "Phiên đăng nhập không còn hợp lệ."
                        429 -> "Bạn thao tác quá nhanh. Vui lòng thử lại sau."
                        in 500..599 -> "Máy chủ đang gặp lỗi. Vui lòng thử lại."
                        else -> "Không thể tải tin tức. Vui lòng thử lại."
                    }

                    is IOException ->
                        "Không thể kết nối máy chủ. Vui lòng kiểm tra mạng."

                    else ->
                        "Không thể tải dữ liệu. Vui lòng thử lại."
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = message
                    )
                }
            }
        }
    }
}
