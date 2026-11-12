package com.coffee.gu.menu

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class IngredientManager(
    private val ingredientRepository: IngredientRepository,
) {
    @Transactional
    fun updateAvailability(storeId: Long, ingredientId: Long, isAvailable: Boolean): Ingredient {
        val ingredient = ingredientRepository.findByStoreIdAndIngredientId(storeId, ingredientId)
            ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
        ingredient.updateAvailability(isAvailable)
        return ingredientRepository.save(ingredient)
    }

    @Transactional
    fun updateStockQuantity(storeId: Long, ingredientId: Long, stockQuantity: Int): Ingredient {
        val ingredient = ingredientRepository.findByStoreIdAndIngredientId(storeId, ingredientId)
            ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
        ingredient.updateStockQuantity(stockQuantity)
        return ingredientRepository.save(ingredient)
    }
}
