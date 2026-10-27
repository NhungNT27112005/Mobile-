package com.example.mobile.feature.home.domain

import com.example.mobile.core.model.Category

class GetCategoriesUseCase(
    private val repository: HomeRepository
) {

    suspend operator fun invoke(): List<Category> {
        return repository.getCategories()
    }
}
