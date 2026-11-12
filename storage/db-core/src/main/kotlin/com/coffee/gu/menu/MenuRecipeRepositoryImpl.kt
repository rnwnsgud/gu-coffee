package com.coffee.gu.menu

import org.springframework.stereotype.Repository

@Repository
class MenuRecipeRepositoryImpl(
    private val menuRecipeJpaRepository: MenuRecipeJpaRepository,
) : MenuRecipeRepository {

    override fun findByMenuId(menuId: Long): List<MenuRecipe> {
        return menuRecipeJpaRepository.findByMenuId(menuId).map { it.toModel() }
    }

    override fun findAllByMenuIdIn(menuIds: Collection<Long>): List<MenuRecipe> {
        if (menuIds.isEmpty()) return emptyList()
        return menuRecipeJpaRepository.findAllByMenuIdIn(menuIds).map { it.toModel() }
    }

    override fun save(menuRecipe: MenuRecipe): MenuRecipe {
        val savedMenuRecipe = menuRecipeJpaRepository.save(MenuRecipeEntity.from(menuRecipe))
        return savedMenuRecipe.toModel()
    }

    override fun saveAll(menuRecipes: Collection<MenuRecipe>): List<MenuRecipe> {
        if (menuRecipes.isEmpty()) return emptyList()
        val savedMenuRecipes = menuRecipeJpaRepository.saveAll(menuRecipes.map { MenuRecipeEntity.from(it) })
        return savedMenuRecipes.map { it.toModel() }
    }
}
