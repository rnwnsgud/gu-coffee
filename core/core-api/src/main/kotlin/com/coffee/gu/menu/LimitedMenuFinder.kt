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
    @Cacheable(cacheNames = [CacheConfig.LIMITED_MENU_POLICIES_CACHE], sync = true)
    fun getAllActivePolicies(): List<LimitedMenuPolicy> {
        return limitedMenuRepository.findAllPolicies()
    }

    fun getByMenuId(menuId: Long): LimitedMenu {
        return limitedMenuRepository.findByMenuId(menuId)
            ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
    }
}
