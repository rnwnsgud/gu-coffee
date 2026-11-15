package com.coffee.gu.menu

import org.springframework.stereotype.Component

@Component
class MenuAvailabilityReader(
    private val menuRecipeRepository: MenuRecipeRepository,
    private val ingredientRepository: IngredientRepository,
) {
    fun findSoldOutMenuIds(storeId: Long, menuIds: Collection<Long>): Set<Long> {
        if (menuIds.isEmpty()) return emptySet()

        val recipes = menuRecipeRepository.findAllByMenuIdIn(menuIds)
        if (recipes.isEmpty()) return emptySet()

        val ingredientIds = recipes.map { it.ingredientId }.toSet()
        val ingredients = ingredientRepository.findAllByStoreIdAndIngredientIdIn(
            storeId = storeId,
            ingredientIds = ingredientIds,
        ).associateBy { it.ingredientId }

        val soldOutMenuIds = mutableSetOf<Long>()
        val recipesByMenuId = recipes.groupBy { it.menuId }

        for ((menuId, menuRecipes) in recipesByMenuId) {
            val isSoldOut = menuRecipes.any { recipe ->
                val ingredient = ingredients[recipe.ingredientId]
                ingredient == null || !ingredient.isAvailable
            }
            if (isSoldOut) {
                soldOutMenuIds.add(menuId)
            }
        }

        return soldOutMenuIds
    }
}
