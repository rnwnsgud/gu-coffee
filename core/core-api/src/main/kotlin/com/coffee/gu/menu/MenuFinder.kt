package com.coffee.gu.menu

import com.coffee.gu.OffsetLimit
import com.coffee.gu.Page
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class MenuFinder(
    private val menuRepository: MenuRepository
) {
    fun findByCategory(categoryId: Long, offsetLimit: OffsetLimit): Page<Menu> {
        val pageSize = offsetLimit.limit
        val menus = menuRepository.findByCategoryId(categoryId, offsetLimit.withLimit(pageSize + 1))
        return Page.of(menus, pageSize)
    }

    fun findByCategory(categoryId: Long, pageSize: Int, cursor: LocalDateTime? = null, lastId: Long? = null): Page<Menu> {
        val menus = menuRepository.findByCategoryId(categoryId, cursor, lastId, pageSize)
        return Page.of(
            items = menus,
            pageSize = pageSize,
            cursorExtractor = { it.createdAt },
            idExtractor = { it.id },
        )
    }

    fun getById(menuId: Long): Menu {
        return menuRepository.findById(menuId)
    }

    fun findAllByIdIn(menuIds: List<Long>): List<Menu> {
        return menuRepository.findAllByIdIn(menuIds)
    }
}
