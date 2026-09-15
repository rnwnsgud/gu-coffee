package com.coffee.gu.order

import com.coffee.gu.menu.Menu

class OrderMenus(
    val menuMap: Map<Long, Menu>,
) {
    fun getByMenuId(menuId: Long): Menu? {
        return menuMap[menuId]
    }

    fun matches(menuIds: Set<Long>): Boolean {
        return menuMap.keys == menuIds
    }

    companion object {
        fun from(menus: List<Menu>): OrderMenus {
            return OrderMenus(menus.associateBy { it.id })
        }
    }
}