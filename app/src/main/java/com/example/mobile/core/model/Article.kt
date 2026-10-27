package com.example.mobile.core.model

data class Article(
    val id: String,
    val title: String,
    val summary: String,
    val categoryId: String,
    val sourceName: String,
    val publishedAt: String,
    val imageUrl: String? = null,
    val content: String? = null
)