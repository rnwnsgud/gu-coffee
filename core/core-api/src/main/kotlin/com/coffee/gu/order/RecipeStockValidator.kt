package com.coffee.gu.order

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.menu.IngredientRepository
import com.coffee.gu.menu.MenuRecipeRepository
import org.springframework.stereotype.Component

@Component
class RecipeStockValidator(
    private val menuRecipeRepository: MenuRecipeRepository,
    private val ingredientRepository: IngredientRepository,
) {
    fun validateStock(storeId: Long, menuQuantities: Map<Long, Int>) {
        val activeMenuIds = menuQuantities.filterValues { it > 0 }.keys
        if (activeMenuIds.isEmpty()) return

        val recipes = menuRecipeRepository.findAllByMenuIdIn(activeMenuIds)
        if (recipes.isEmpty()) return

        val requiredIngredientIds = recipes.map { it.ingredientId }.toSet()
        val ingredients = ingredientRepository.findAllByStoreIdAndIngredientIdIn(
            storeId = storeId,
            ingredientIds = requiredIngredientIds,
        ).associateBy { it.ingredientId }

        for (ingredientId in requiredIngredientIds) {
            val ingredient = ingredients[ingredientId]
            if (ingredient == null || !ingredient.isAvailable) {
                throw CoreException(ErrorType.MENU_INGREDIENT_OUT_OF_STOCK)
            }
        }
    }
}
