package com.example.mobile.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile.feature.search.domain.GetSearchCategoriesUseCase
import com.example.mobile.feature.search.domain.SearchArticlesUseCase
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

class SearchViewModel(
    private val getSearchCategoriesUseCase: GetSearchCategoriesUseCase,
    private val searchArticlesUseCase: SearchArticlesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadResults()
    }

    fun onQueryChanged(query: String) {
        if (query == _uiState.value.query) return

        _uiState.update {
            it.copy(query = query)
        }

        loadResults(debounce = true)
    }

    fun onCategorySelected(categoryId: String?) {
        if (categoryId == _uiState.value.selectedCategoryId) return

        _uiState.update {
            it.copy(selectedCategoryId = categoryId)
        }

        loadResults()
    }

    fun searchNow() {
        loadResults()
    }

    fun retry() {
        if (_uiState.value.isLoading) return

        loadResults()
    }

    private fun loadResults(debounce: Boolean = false) {
        val currentState = _uiState.value
        val query = currentState.query
        val categoryId = currentState.selectedCategoryId

        searchJob?.cancel()

        _uiState.update {
            it.copy(
                isLoading = true,
                articles = emptyList(),
                errorMessage = null
            )
        }

        searchJob = viewModelScope.launch {
            try {
                if (debounce) {
                    delay(300)
                }

                if (_uiState.value.categories.isEmpty()) {
                    val categories = getSearchCategoriesUseCase()

                    _uiState.update {
                        it.copy(categories = categories)
                    }
                }

                val articles = searchArticlesUseCase(
                    query = query,
                    categoryId = categoryId
                )

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
                        400, 422 -> "Thông tin tìm kiếm không hợp lệ."
                        401 -> "Phiên đăng nhập không còn hợp lệ."
                        429 -> "Bạn thao tác quá nhanh. Vui lòng thử lại sau."
                        in 500..599 ->
                            "Máy chủ đang gặp lỗi. Vui lòng thử lại."

                        else ->
                            "Không thể tìm kiếm. Vui lòng thử lại."
                    }

                    is IOException ->
                        "Không thể kết nối máy chủ. Vui lòng kiểm tra mạng."

                    is IllegalArgumentException ->
                        e.message ?: "Từ khóa tìm kiếm không hợp lệ."

                    else ->
                        "Không thể tải kết quả. Vui lòng thử lại."
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
