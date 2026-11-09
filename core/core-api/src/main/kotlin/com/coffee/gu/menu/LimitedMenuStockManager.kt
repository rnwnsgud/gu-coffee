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
        val policies = findLimitedMenuPoliciesInOrder(menuQuantities).sortedBy { it.menuId }
        if (policies.isEmpty()) return

        policies.forEach { policy ->
            val requestQuantity = menuQuantities[policy.menuId] ?: return@forEach
            policy.validateOrderQuantity(requestQuantity)
        }

        val reservedMenus = mutableListOf<Pair<Long, Int>>()
        try {
            for (policy in policies) {
                val reqQty = menuQuantities[policy.menuId] ?: continue
                val result = reservationManager.reserve(
                    menuId = policy.menuId,
                    orderKey = orderKey,
                    principalKey = principalKey,
                    quantity = reqQty,
                    ttlMillis = ttlMillis,
                    nowEpochMillis = nowEpochMillis,
                ) {
                    limitedMenuFinder.getByMenuId(policy.menuId).remainingQuantity
                }

                when (result) {
                    ReservationResult.RESERVED -> reservedMenus.add(policy.menuId to reqQty)
                    ReservationResult.SOLD_OUT -> throw CoreException(ErrorType.LIMITED_MENU_SOLD_OUT)
                    ReservationResult.ALREADY_PURCHASED -> throw CoreException(ErrorType.LIMITED_MENU_ALREADY_PURCHASED)
                }
            }
        } catch (e: Exception) {
            for ((reservedMenuId, reservedQty) in reservedMenus) {
                reservationManager.release(
                    menuId = reservedMenuId,
                    orderKey = orderKey,
                    principalKey = principalKey,
                    quantity = reservedQty,
                    nowEpochMillis = nowEpochMillis,
                )
            }
            throw e
        }
    }

    fun verifyAndConfirmIfLimited(
        orderKey: String,
        principalKey: String,
        menuQuantities: Map<Long, Int>,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): ConfirmationResult {
        val policies = findLimitedMenuPoliciesInOrder(menuQuantities).sortedBy { it.menuId }
        if (policies.isEmpty()) return ConfirmationResult.CONFIRMED

        policies.forEach { policy ->
            val requestQuantity = menuQuantities[policy.menuId] ?: return@forEach
            policy.validateOrderQuantity(requestQuantity)
        }

        val confirmedMenus = mutableListOf<Pair<Long, Int>>()
        var finalResult = ConfirmationResult.CONFIRMED
        for ((menuId) in policies) {
            val quantity = menuQuantities[menuId] ?: continue
            val result = reservationManager.verifyAndConfirm(
                menuId = menuId,
                orderKey = orderKey,
                nowEpochMillis = nowEpochMillis,
            )
            if (result == ConfirmationResult.EXPIRED) {
                finalResult = ConfirmationResult.EXPIRED
            } else {
                confirmedMenus.add(menuId to quantity)
            }
        }
        if (finalResult == ConfirmationResult.EXPIRED) {
            for ((confirmedMenuId, confirmedQty) in confirmedMenus) {
                reservationManager.release(
                    menuId = confirmedMenuId,
                    orderKey = orderKey,
                    principalKey = principalKey,
                    quantity = confirmedQty,
                    nowEpochMillis = nowEpochMillis,
                )
            }
        }
        return finalResult
    }

    fun confirmSoldQuantityInDbIfLimited(menuQuantities: Map<Long, Int>) {
        val limitedMenuIds = findLimitedMenuIdsInOrder(menuQuantities)
        if (limitedMenuIds.isEmpty()) return

        for (menuId in limitedMenuIds) {
            val quantity = menuQuantities[menuId] ?: continue
            limitedMenuManager.increaseSoldQuantity(menuId, quantity)
        }
    }

    fun releaseIfLimited(
        orderKey: String,
        principalKey: String,
        menuQuantities: Map<Long, Int>,
        restoreDbSoldQuantity: Boolean = false,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ) {
        val limitedMenuIds = findLimitedMenuIdsInOrder(menuQuantities)
        if (limitedMenuIds.isEmpty()) return

        for (menuId in limitedMenuIds) {
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

    private fun findLimitedMenuPoliciesInOrder(menuQuantities: Map<Long, Int>): List<LimitedMenuPolicy> {
        if (menuQuantities.isEmpty()) return emptyList()
        val policies = limitedMenuFinder.getAllActivePolicies()
        val requestedIds = menuQuantities.keys
        return policies.filter { it.menuId in requestedIds }
    }

    private fun findLimitedMenuIdsInOrder(menuQuantities: Map<Long, Int>): List<Long> {
        if (menuQuantities.isEmpty()) return emptyList()
        val policies = limitedMenuFinder.getAllActivePolicies()
        val requestedIds = menuQuantities.keys
        return policies.map { it.menuId }.filter { it in requestedIds }
    }
}
