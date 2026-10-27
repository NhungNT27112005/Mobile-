package com.example.mobile.feature.search.domain

import com.example.mobile.core.model.Article

class SearchArticlesUseCase(
    private val repository: SearchRepository
) {

    suspend operator fun invoke(
        query: String,
        categoryId: String? = null
    ): List<Article> {
        val cleanQuery = query.trim()

        require(cleanQuery.length <= 200) {
            "Từ khóa tìm kiếm tối đa 200 ký tự."
        }

        return repository.searchArticles(
            query = cleanQuery,
            categoryId = categoryId
        )
    }
}
