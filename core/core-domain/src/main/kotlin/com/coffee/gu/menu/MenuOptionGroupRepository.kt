package com.coffee.gu.menu

interface MenuOptionGroupRepository {
    fun findByMenuId(menuId: Long): List<MenuOptionGroup>
}
