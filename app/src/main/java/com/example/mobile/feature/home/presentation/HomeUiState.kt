package com.example.mobile.feature.home.presentation

import com.example.mobile.core.model.Article
import com.example.mobile.core.model.Category

data class HomeUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val articles: List<Article> = emptyList(),
    val selectedCategoryId: String? = null,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading &&
            errorMessage == null &&
            articles.isEmpty()
}
