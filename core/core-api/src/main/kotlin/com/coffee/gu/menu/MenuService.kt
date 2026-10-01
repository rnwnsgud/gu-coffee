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
    private val optionFinder: OptionFinder
) {
    fun findMenus(categoryId: Long, offsetLimit: OffsetLimit): Page<Menu> {
        return menuFinder.findByCategory(categoryId, offsetLimit)
    }

    fun findMenus(categoryId: Long, pageSize: Int, cursor: LocalDateTime, lastId: Long): Page<Menu> {
        return menuFinder.findByCategory(categoryId, pageSize, cursor, lastId)
    }

    @Cacheable(cacheNames = [CacheConfig.MENU_DETAIL_CACHE], key = "#menuId", sync = true)
    fun getMenu(menuId: Long): MenuDetailResult {
        val menu = menuFinder.getById(menuId)
        val optionGroups = optionFinder.findByMenuId(menuId)
        val options = optionFinder.findByOptionGroups(optionGroups)
        return MenuDetailResult(menu, optionGroups, options)
    }

    @CacheEvict(cacheNames = [CacheConfig.MENU_DETAIL_CACHE], key = "#menuId")
    fun evictMenuDetail(menuId: Long) {
    }
}
