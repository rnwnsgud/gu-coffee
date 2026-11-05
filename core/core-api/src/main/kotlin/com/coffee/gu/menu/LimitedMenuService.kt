package com.coffee.gu.menu

import org.springframework.stereotype.Service

@Service
class LimitedMenuService(
    private val limitedMenuFinder: LimitedMenuFinder,
    private val limitedMenuStockManager: LimitedMenuStockManager,
) {
    fun getByMenuId(menuId: Long): LimitedMenu {
        return limitedMenuFinder.getByMenuId(menuId)
    }

    fun getAvailableStock(
        menuId: Long,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): Int {
        return limitedMenuStockManager.getAvailableStock(menuId, nowEpochMillis)
    }
}
