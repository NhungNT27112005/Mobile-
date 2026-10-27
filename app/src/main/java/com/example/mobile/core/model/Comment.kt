
package com.example.mobile.core.model

data class Comment(
    val id: String,
    val articleId: String,
    val userId: String,
    val displayName: String,
    val content: String,
    val createdAt: String
)
