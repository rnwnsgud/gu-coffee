package com.coffee.gu.menu

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.lock.LimitedMenuStockReservationManager
import com.coffee.gu.lock.LimitedMenuStockReservationManager.ConfirmationResult
import com.coffee.gu.lock.LimitedMenuStockReservationManager.ReservationResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class LimitedMenuStockManager(
    private val limitedMenuFinder: LimitedMenuFinder,
    private val limitedMenuManager: LimitedMenuManager,
    private val reservationManager: LimitedMenuStockReservationManager,
    @param:Value("\${limited-menu.reservation-ttl-millis:180000}")
    private val defaultReservationTtlMillis: Long = LimitedMenuStockReservationManager.DEFAULT_RESERVATION_TTL_MILLIS,
) {
    fun getAvailableStock(
        menuId: Long,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): Int {
        return reservationManager.getAvailableStock(menuId, nowEpochMillis)
    }

    fun initializeStock(menuId: Long, totalQuantity: Int) {
        reservationManager.initializeStock(menuId, totalQuantity)
    }

    fun reserveIfLimited(
        orderKey: String,
        principalKey: String,
        menuQuantities: Map<Long, Int>,
        ttlMillis: Long = defaultReservationTtlMillis,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ) {
        val limitedMenus = findLimitedMenusInOrder(menuQuantities)
        if (limitedMenus.isEmpty()) return

        if (limitedMenus.size > 1) {
            throw CoreException(ErrorType.LIMITED_MENU_MULTIPLE_ITEMS_NOT_ALLOWED)
        }

        for (limitedMenu in limitedMenus) {
            val menuId = limitedMenu.menu.id
            val reqQty = menuQuantities[menuId] ?: continue
            if (reqQty > limitedMenu.maxPurchasePerUser) {
                throw CoreException(ErrorType.LIMITED_MENU_EXCEED_MAX_PURCHASE_QUANTITY)
            }
        }

        val limitedMenu = limitedMenus.first()
        val menuId = limitedMenu.menu.id
        val reqQty = menuQuantities[menuId] ?: return

        val result = reservationManager.reserve(
            menuId = menuId,
            orderKey = orderKey,
            principalKey = principalKey,
            quantity = reqQty,
            ttlMillis = ttlMillis,
            nowEpochMillis = nowEpochMillis,
        ) {
            limitedMenu.remainingQuantity
        }

        when (result) {
            ReservationResult.RESERVED -> return
            ReservationResult.SOLD_OUT -> throw CoreException(ErrorType.LIMITED_MENU_SOLD_OUT)
            ReservationResult.ALREADY_PURCHASED -> throw CoreException(ErrorType.LIMITED_MENU_ALREADY_PURCHASED)
        }
    }

    fun verifyAndConfirmIfLimited(
        orderKey: String,
        menuQuantities: Map<Long, Int>,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): ConfirmationResult {
        val limitedMenus = findLimitedMenusInOrder(menuQuantities)
        if (limitedMenus.isEmpty()) return ConfirmationResult.CONFIRMED

        for (limitedMenu in limitedMenus) {
            val quantity = menuQuantities[limitedMenu.menu.id] ?: continue
            if (quantity > limitedMenu.maxPurchasePerUser) {
                throw CoreException(ErrorType.LIMITED_MENU_EXCEED_MAX_PURCHASE_QUANTITY)
            }
        }

        var finalResult = ConfirmationResult.CONFIRMED
        for (limitedMenu in limitedMenus) {
            val result = reservationManager.verifyAndConfirm(
                menuId = limitedMenu.menu.id,
                orderKey = orderKey,
                nowEpochMillis = nowEpochMillis,
            )
            if (result == ConfirmationResult.EXPIRED) {
                finalResult = ConfirmationResult.EXPIRED
            }
        }
        return finalResult
    }

    fun confirmSoldQuantityInDbIfLimited(menuQuantities: Map<Long, Int>) {
        val limitedMenus = findLimitedMenusInOrder(menuQuantities)
        if (limitedMenus.isEmpty()) return

        for (limitedMenu in limitedMenus) {
            val quantity = menuQuantities[limitedMenu.menu.id] ?: continue
            limitedMenuManager.increaseSoldQuantity(limitedMenu.menu.id, quantity)
        }
    }

    fun releaseIfLimited(
        orderKey: String,
        principalKey: String,
        menuQuantities: Map<Long, Int>,
        restoreDbSoldQuantity: Boolean = false,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ) {
        val limitedMenus = findLimitedMenusInOrder(menuQuantities)
        if (limitedMenus.isEmpty()) return

        for (limitedMenu in limitedMenus) {
            val menuId = limitedMenu.menu.id
            val quantity = menuQuantities[menuId] ?: continue
            if (restoreDbSoldQuantity) {
                limitedMenuManager.decreaseSoldQuantity(menuId, quantity)
            }
            reservationManager.release(
                menuId = menuId,
                orderKey = orderKey,
                principalKey = principalKey,
                quantity = quantity,
                nowEpochMillis = nowEpochMillis,
            )
        }
    }

    private fun findLimitedMenusInOrder(menuQuantities: Map<Long, Int>): List<LimitedMenu> {
        if (menuQuantities.isEmpty()) return emptyList()
        val activeMenuIds = limitedMenuFinder.getAllActiveMenuIds()
        val requestedLimitedMenuIds = menuQuantities.keys.filter { it in activeMenuIds }
        if (requestedLimitedMenuIds.isEmpty()) return emptyList()
        return limitedMenuFinder.findAllByMenuIdIn(requestedLimitedMenuIds)
    }
}
