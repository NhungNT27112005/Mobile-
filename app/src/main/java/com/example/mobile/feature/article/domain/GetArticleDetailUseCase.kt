package com.example.mobile.feature.article.domain

import com.example.mobile.core.model.Article

class GetArticleDetailUseCase(
    private val repository: ArticleRepository
) {

    suspend operator fun invoke(articleId: String): Article {
        require(articleId.isNotBlank()) {
            "Mã bài viết không được để trống."
        }

        return repository.getArticleDetail(articleId)
    }
}
