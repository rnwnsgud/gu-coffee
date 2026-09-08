package com.coffee.gu.menu

import org.springframework.data.jpa.repository.JpaRepository

interface MenuOptionGroupJpaRepository : JpaRepository<MenuOptionGroupEntity, Long> {
    fun findByMenuId(menuId: Long): List<MenuOptionGroupEntity>
}
