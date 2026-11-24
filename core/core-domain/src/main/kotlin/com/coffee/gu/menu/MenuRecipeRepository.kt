package com.coffee.gu.menu

interface MenuRecipeRepository {
    fun findAllByMenuIdIn(menuIds: Collection<Long>): List<MenuRecipe>
    fun saveAll(menuRecipes: Collection<MenuRecipe>): List<MenuRecipe>
}
