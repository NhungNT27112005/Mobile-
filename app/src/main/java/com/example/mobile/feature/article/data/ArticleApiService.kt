package com.example.mobile.feature.article.data

import com.example.mobile.core.model.Article
import retrofit2.http.GET
import retrofit2.http.Path

interface ArticleApiService {

    @GET("articles/{articleId}")
    suspend fun getArticleDetail(
        @Path("articleId") articleId: String
    ): Article
}
