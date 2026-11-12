package com.coffee.gu.menu

import org.springframework.stereotype.Component

@Component
class IngredientFinder(
    private val ingredientRepository: IngredientRepository,
) {
    fun findAllByStoreId(storeId: Long): List<Ingredient> {
        return ingredientRepository.findAllByStoreId(storeId)
    }
}
