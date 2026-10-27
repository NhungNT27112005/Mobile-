package com.example.mobile.feature.home.domain

import com.example.mobile.core.model.Article
import com.example.mobile.core.model.Category

interface HomeRepository {

    suspend fun getCategories(): List<Category>

    suspend fun getArticles(
        categoryId: String? = null
    ): List<Article>
}
