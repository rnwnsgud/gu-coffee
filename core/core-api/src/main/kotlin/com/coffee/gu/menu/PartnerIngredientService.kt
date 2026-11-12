package com.coffee.gu.menu

import org.springframework.stereotype.Service

@Service
class PartnerIngredientService(
    private val ingredientFinder: IngredientFinder,
    private val ingredientManager: IngredientManager,
) {
    fun getStoreIngredients(storeId: Long): List<Ingredient> {
        return ingredientFinder.findAllByStoreId(storeId)
    }

    fun updateAvailability(storeId: Long, ingredientId: Long, isAvailable: Boolean): Ingredient {
        return ingredientManager.updateAvailability(storeId, ingredientId, isAvailable)
    }

    fun updateStockQuantity(storeId: Long, ingredientId: Long, stockQuantity: Int): Ingredient {
        return ingredientManager.updateStockQuantity(storeId, ingredientId, stockQuantity)
    }
}
