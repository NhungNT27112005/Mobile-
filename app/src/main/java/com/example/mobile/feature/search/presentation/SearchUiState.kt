package com.example.mobile.feature.search.presentation

import com.example.mobile.core.model.Article
import com.example.mobile.core.model.Category

data class SearchUiState(
    val query: String = "",
    val selectedCategoryId: String? = null,
    val categories: List<Category> = emptyList(),
    val articles: List<Article> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading &&
            errorMessage == null &&
            articles.isEmpty()
}
