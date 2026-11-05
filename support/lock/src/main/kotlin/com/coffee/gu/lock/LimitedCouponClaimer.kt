package com.coffee.gu.lock

interface LimitedCouponClaimer {

    fun claim(
        limitedCouponId: Long,
        principalKey: String,
        stockSupplier: (() -> Int)? = null,
    ): ClaimResult

    fun initializeStock(limitedCouponId: Long, totalQuantity: Int)

    fun rollbackClaim(limitedCouponId: Long, principalKey: String)

    fun getRemainingStock(limitedCouponId: Long): Int

    enum class ClaimResult {
        SUCCESS,
        SOLD_OUT,
        ALREADY_CLAIMED,
    }
}
