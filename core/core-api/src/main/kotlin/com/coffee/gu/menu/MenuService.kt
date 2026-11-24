package com.coffee.gu.menu

import com.coffee.gu.OffsetLimit
import com.coffee.gu.Page
import com.coffee.gu.config.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class MenuService(
    private val menuFinder: MenuFinder,
    private val optionFinder: OptionFinder,
    private val menuAvailabilityReader: MenuAvailabilityReader,
) {
    fun findMenus(categoryId: Long, offsetLimit: OffsetLimit, storeId: Long? = null): Page<MenuItemResult> {
        val page = menuFinder.findByCategory(categoryId, offsetLimit)
        return toMenuItemPage(page, storeId)
    }

    fun findMenus(
        categoryId: Long,
        pageSize: Int,
        cursor: LocalDateTime? = null,
        lastId: Long? = null,
        storeId: Long? = null,
    ): Page<MenuItemResult> {
        val page = menuFinder.findByCategory(categoryId, pageSize, cursor, lastId)
        return toMenuItemPage(page, storeId)
    }

    private fun toMenuItemPage(page: Page<Menu>, storeId: Long?): Page<MenuItemResult> {
        val soldOutMenuIds = if (storeId != null && page.content.isNotEmpty()) {
            menuAvailabilityReader.findSoldOutMenuIds(storeId, page.content.map { it.id })
        } else {
            emptySet()
        }
        val items = page.content.map { menu ->
            MenuItemResult(
                menu = menu,
                isSoldOut = menu.id in soldOutMenuIds,
            )
        }
        return Page(
            content = items,
            hasNext = page.hasNext,
            nextCursor = page.nextCursor,
            nextLastId = page.nextLastId,
        )
    }

    @Cacheable(cacheNames = [CacheConfig.MENU_DETAIL_CACHE], key = "#menuId", sync = true)
    fun getMenu(menuId: Long): MenuDetailResult {
        val menu = menuFinder.getById(menuId)
        val optionGroups = optionFinder.findByMenuId(menuId)
        val options = optionFinder.findByOptionGroups(optionGroups)
        return MenuDetailResult(menu, optionGroups, options)
    }
}
