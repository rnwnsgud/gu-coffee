package com.coffee.gu.menu

import org.springframework.stereotype.Repository

@Repository
class MenuRecipeRepositoryImpl(
    private val menuRecipeJpaRepository: MenuRecipeJpaRepository,
) : MenuRecipeRepository {

    override fun findAllByMenuIdIn(menuIds: Collection<Long>): List<MenuRecipe> {
        if (menuIds.isEmpty()) return emptyList()
        return menuRecipeJpaRepository.findAllByMenuIdIn(menuIds).map { it.toModel() }
    }

    override fun saveAll(menuRecipes: Collection<MenuRecipe>): List<MenuRecipe> {
        if (menuRecipes.isEmpty()) return emptyList()
        val savedMenuRecipes = menuRecipeJpaRepository.saveAll(menuRecipes.map { MenuRecipeEntity.from(it) })
        return savedMenuRecipes.map { it.toModel() }
    }
}
