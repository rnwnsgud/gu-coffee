package com.coffee.gu.partner.menu

import com.coffee.gu.menu.Ingredient
import com.coffee.gu.menu.IngredientRepository
import org.springframework.stereotype.Component

@Component
class IngredientFinder(
    private val ingredientRepository: IngredientRepository,
) {
    fun findAllByStoreId(storeId: Long): List<Ingredient> {
        return ingredientRepository.findAllByStoreId(storeId)
    }
}
