package com.coffee.gu.menu

import org.springframework.data.jpa.repository.JpaRepository

interface MenuRecipeJpaRepository : JpaRepository<MenuRecipeEntity, Long> {
    fun findAllByMenuIdIn(menuIds: Collection<Long>): List<MenuRecipeEntity>
}
