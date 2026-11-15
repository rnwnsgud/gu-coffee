package com.coffee.gu.partner.menu

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.menu.Ingredient
import com.coffee.gu.menu.IngredientRepository
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
}
