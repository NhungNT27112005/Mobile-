package com.example.mobile.feature.article.domain

import com.example.mobile.core.model.Article

interface ArticleRepository {

    suspend fun getArticleDetail(articleId: String): Article
}
