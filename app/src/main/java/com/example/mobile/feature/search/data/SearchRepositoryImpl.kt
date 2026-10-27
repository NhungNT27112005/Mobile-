package com.example.mobile.feature.search.data

import com.example.mobile.core.model.Article
import com.example.mobile.core.model.Category
import com.example.mobile.feature.home.data.NewsApiService
import com.example.mobile.feature.search.domain.SearchRepository

class SearchRepositoryImpl(
    private val apiService: NewsApiService
) : SearchRepository {

    override suspend fun getCategories(): List<Category> {
        return apiService.getCategories()
    }

    override suspend fun searchArticles(
        query: String,
        categoryId: String?
    ): List<Article> {
        return apiService.getArticles(
            categoryId = categoryId,
            query = query
        )
    }
}
