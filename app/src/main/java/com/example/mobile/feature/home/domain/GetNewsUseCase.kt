package com.example.mobile.feature.home.domain

import com.example.mobile.core.model.Article

class GetNewsUseCase(
    private val repository: HomeRepository
) {

    suspend operator fun invoke(
        categoryId: String? = null
    ): List<Article> {
        return repository.getArticles(categoryId)
    }
}
