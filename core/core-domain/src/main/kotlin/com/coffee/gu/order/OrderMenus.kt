package com.coffee.gu.order

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.menu.Menu

class OrderMenus(
    private val menuMap: Map<Long, Menu>,
) {
    fun getByMenuId(menuId: Long): Menu {
        return menuMap[menuId] ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
    }

    companion object {
        fun from(menus: List<Menu>, requestedMenuIds: Set<Long>): OrderMenus {
            if (menus.isEmpty()) throw CoreException(ErrorType.NOT_FOUND_DATA)
            val menuMap = menus.associateBy { it.id }
            if (menuMap.keys != requestedMenuIds) throw CoreException(ErrorType.MENU_MISMATCH_IN_ORDER)
            return OrderMenus(menuMap)
        }
    }
}