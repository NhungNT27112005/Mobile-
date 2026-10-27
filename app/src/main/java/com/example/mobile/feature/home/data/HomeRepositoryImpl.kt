package com.example.mobile.feature.home.data

import com.example.mobile.core.model.Article
import com.example.mobile.core.model.Category
import com.example.mobile.feature.home.domain.HomeRepository

class HomeRepositoryImpl(
    private val apiService: NewsApiService
) : HomeRepository {

    override suspend fun getCategories(): List<Category> {
        return apiService.getCategories()
    }

    override suspend fun getArticles(
        categoryId: String?
    ): List<Article> {
        return apiService.getArticles(categoryId)
    }
}
