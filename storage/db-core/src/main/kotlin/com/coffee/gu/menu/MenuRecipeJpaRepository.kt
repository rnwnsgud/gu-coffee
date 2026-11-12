package com.coffee.gu.menu

import org.springframework.data.jpa.repository.JpaRepository

interface MenuRecipeJpaRepository : JpaRepository<MenuRecipeEntity, Long> {
    fun findByMenuId(menuId: Long): List<MenuRecipeEntity>
    fun findAllByMenuIdIn(menuIds: Collection<Long>): List<MenuRecipeEntity>
}
