package com.coffee.gu.menu

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.config.CacheConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class LimitedMenuFinder(
    private val limitedMenuRepository: LimitedMenuRepository,
) {
    @Cacheable(cacheNames = [CacheConfig.LIMITED_MENU_IDS_CACHE], sync = true)
    fun getAllActiveMenuIds(): List<Long> {
        return limitedMenuRepository.findAllMenuIds()
    }

    fun getByMenuId(menuId: Long): LimitedMenu {
        return limitedMenuRepository.findByMenuId(menuId)
            ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
    }

    fun findAllByMenuIdIn(menuIds: Collection<Long>): List<LimitedMenu> {
        return limitedMenuRepository.findAllByMenuIdIn(menuIds)
    }
}
