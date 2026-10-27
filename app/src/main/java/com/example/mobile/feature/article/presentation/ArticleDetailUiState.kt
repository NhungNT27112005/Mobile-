package com.example.mobile.feature.article.presentation

import com.example.mobile.core.model.Article

data class ArticleDetailUiState(
    val isLoading: Boolean = true,
    val article: Article? = null,
    val errorMessage: String? = null
)
