package com.example.mobile.feature.article.data

import com.example.mobile.core.model.Article
import com.example.mobile.feature.article.domain.ArticleRepository

class ArticleRepositoryImpl(
    private val apiService: ArticleApiService
) : ArticleRepository {

    override suspend fun getArticleDetail(
        articleId: String
    ): Article {
        return apiService.getArticleDetail(articleId)
    }
}
