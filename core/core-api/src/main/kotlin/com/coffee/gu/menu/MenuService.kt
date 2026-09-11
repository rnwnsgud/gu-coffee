package com.coffee.gu.menu

import com.coffee.gu.OffsetLimit
import com.coffee.gu.Page
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class MenuService(
    private val menuFinder: MenuFinder,
    private val optionFinder: OptionFinder
) {
    fun findMenus(categoryId: Long, offsetLimit: OffsetLimit): Page<Menu> {
        return menuFinder.findByCategory(categoryId, offsetLimit)
    }

    fun findMenus(categoryId: Long, pageSize: Int, cursor: LocalDateTime, lastId: Long): Page<Menu> {
        return menuFinder.findByCategory(categoryId, pageSize, cursor, lastId)
    }

    fun getMenu(menuId: Long): MenuDetailResult {
        val menu = menuFinder.getById(menuId)
        val optionGroups = optionFinder.findByMenuId(menuId)
        val options = optionFinder.findByOptionGroups(optionGroups)
        return MenuDetailResult(menu, optionGroups, options)
    }
}
