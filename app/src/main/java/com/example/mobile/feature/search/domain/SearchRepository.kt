package com.example.mobile.feature.search.domain

import com.example.mobile.core.model.Article
import com.example.mobile.core.model.Category

interface SearchRepository {

    suspend fun getCategories(): List<Category>

    suspend fun searchArticles(
        query: String,
        categoryId: String? = null
    ): List<Article>
}
