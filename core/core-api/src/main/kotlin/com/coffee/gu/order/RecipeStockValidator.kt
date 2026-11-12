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
        if (menuQuantities.isEmpty()) return

        val recipes = menuRecipeRepository.findAllByMenuIdIn(menuQuantities.keys)
        if (recipes.isEmpty()) return

        val requiredQuantities = mutableMapOf<Long, Int>()
        for (recipe in recipes) {
            val orderQty = menuQuantities[recipe.menuId] ?: 0
            if (orderQty > 0) {
                val totalRequired = recipe.requiredQuantity * orderQty
                requiredQuantities.merge(recipe.ingredientId, totalRequired, Int::plus)
            }
        }

        if (requiredQuantities.isEmpty()) return

        val ingredients = ingredientRepository.findAllByStoreIdAndIngredientIdIn(
            storeId = storeId,
            ingredientIds = requiredQuantities.keys,
        ).associateBy { it.ingredientId }

        for ((ingredientId, requiredQty) in requiredQuantities) {
            val ingredient = ingredients[ingredientId]
            if (ingredient == null || !ingredient.canConsume(requiredQty)) {
                throw CoreException(ErrorType.MENU_INGREDIENT_OUT_OF_STOCK)
            }
        }
    }
}
