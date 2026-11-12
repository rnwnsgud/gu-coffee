package com.coffee.gu.menu

interface MenuRecipeRepository {
    fun findByMenuId(menuId: Long): List<MenuRecipe>
    fun findAllByMenuIdIn(menuIds: Collection<Long>): List<MenuRecipe>
    fun save(menuRecipe: MenuRecipe): MenuRecipe
    fun saveAll(menuRecipes: Collection<MenuRecipe>): List<MenuRecipe>
}
