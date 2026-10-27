package com.example.mobile.feature.search.domain

import com.example.mobile.core.model.Category

class GetSearchCategoriesUseCase(
    private val repository: SearchRepository
) {

    suspend operator fun invoke(): List<Category> {
        return repository.getCategories()
    }
}
