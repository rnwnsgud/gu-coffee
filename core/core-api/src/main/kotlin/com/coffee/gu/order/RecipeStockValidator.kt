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
    fun validateAvailability(storeId: Long, menuIds: Set<Long>) {
        if (menuIds.isEmpty()) return

        val recipes = menuRecipeRepository.findAllByMenuIdIn(menuIds)
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
