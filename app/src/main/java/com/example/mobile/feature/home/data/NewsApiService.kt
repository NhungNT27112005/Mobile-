package com.example.mobile.feature.home.data

import com.example.mobile.core.model.Article
import com.example.mobile.core.model.Category
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {

    @GET("categories")
    suspend fun getCategories(): List<Category>

    @GET("articles")
    suspend fun getArticles(
        @Query("categoryId") categoryId: String? = null,
        @Query("q") query: String? = null
    ): List<Article>
}
