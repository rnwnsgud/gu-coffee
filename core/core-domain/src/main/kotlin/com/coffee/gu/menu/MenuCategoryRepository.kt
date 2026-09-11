package com.coffee.gu.menu

interface MenuCategoryRepository {
    fun findAllByMenuIdIn(menuIds: List<Long>): List<MenuCategory>
}
