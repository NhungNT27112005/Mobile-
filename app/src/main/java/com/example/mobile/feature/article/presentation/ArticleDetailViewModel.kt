package com.example.mobile.feature.article.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile.feature.article.domain.GetArticleDetailUseCase
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ArticleDetailViewModel(
    private val articleId: String,
    private val getArticleDetailUseCase: GetArticleDetailUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArticleDetailUiState())
    val uiState: StateFlow<ArticleDetailUiState> =
        _uiState.asStateFlow()

    init {
        loadArticle()
    }

    fun retry() {
        if (_uiState.value.isLoading) return

        loadArticle()
    }

    private fun loadArticle() {
        _uiState.update {
            it.copy(
                isLoading = true,
                article = null,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val article = getArticleDetailUseCase(articleId)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        article = article
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = when (e) {
                    is HttpException -> when (e.code()) {
                        401 -> "Phiên đăng nhập không còn hợp lệ."
                        404 -> "Không tìm thấy bài viết."
                        429 -> "Bạn thao tác quá nhanh. Vui lòng thử lại sau."
                        in 500..599 ->
                            "Máy chủ đang gặp lỗi. Vui lòng thử lại."

                        else ->
                            "Không thể tải bài viết. Vui lòng thử lại."
                    }

                    is IOException ->
                        "Không thể kết nối máy chủ. Vui lòng kiểm tra mạng."

                    is IllegalArgumentException ->
                        "Mã bài viết không hợp lệ."

                    else ->
                        "Không thể tải nội dung bài viết. Vui lòng thử lại."
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
