package com.coffee.gu.coupon

import com.coffee.gu.lock.LimitedCouponClaimer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class LocalLimitedCouponClaimer : LimitedCouponClaimer {
    private val claimedUsers = ConcurrentHashMap<Long, MutableSet<String>>()
    private val stocks = ConcurrentHashMap<Long, AtomicInteger>()

    override fun claim(
        limitedCouponId: Long,
        principalKey: String,
        stockSupplier: (() -> Int)?,
    ): LimitedCouponClaimer.ClaimResult {
        val stock = stocks.computeIfAbsent(limitedCouponId) {
            AtomicInteger(stockSupplier?.invoke() ?: 0)
        }
        val users = claimedUsers.computeIfAbsent(limitedCouponId) {
            ConcurrentHashMap.newKeySet()
        }

        synchronized(this) {
            if (users.contains(principalKey)) {
                return LimitedCouponClaimer.ClaimResult.ALREADY_CLAIMED
            }
            if (stock.get() <= 0) {
                return LimitedCouponClaimer.ClaimResult.SOLD_OUT
            }
            stock.decrementAndGet()
            users.add(principalKey)
            return LimitedCouponClaimer.ClaimResult.SUCCESS
        }
    }

    override fun initializeStock(limitedCouponId: Long, totalQuantity: Int) {
        stocks[limitedCouponId] = AtomicInteger(totalQuantity)
    }

    override fun rollbackClaim(limitedCouponId: Long, principalKey: String) {
        synchronized(this) {
            val users = claimedUsers[limitedCouponId]
            if (users?.remove(principalKey) == true) {
                stocks[limitedCouponId]?.incrementAndGet()
            }
        }
    }

    override fun getRemainingStock(limitedCouponId: Long): Int {
        return stocks[limitedCouponId]?.get()?.coerceAtLeast(0) ?: 0
    }
}
