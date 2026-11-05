package com.coffee.gu.lock

interface LimitedMenuStockReservationManager {

    fun reserve(
        menuId: Long,
        orderKey: String,
        principalKey: String,
        quantity: Int,
        ttlMillis: Long = DEFAULT_RESERVATION_TTL_MILLIS,
        nowEpochMillis: Long = System.currentTimeMillis(),
        stockSupplier: (() -> Int)? = null,
    ): ReservationResult

    fun verifyAndConfirm(
        menuId: Long,
        orderKey: String,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): ConfirmationResult

    fun release(
        menuId: Long,
        orderKey: String,
        principalKey: String,
        quantity: Int,
        nowEpochMillis: Long = System.currentTimeMillis(),
    )

    fun initializeStock(menuId: Long, totalQuantity: Int)

    fun getAvailableStock(
        menuId: Long,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): Int

    enum class ReservationResult {
        RESERVED,
        SOLD_OUT,
        ALREADY_PURCHASED,
    }

    enum class ConfirmationResult {
        CONFIRMED,
        EXPIRED,
    }

    companion object {
        const val DEFAULT_RESERVATION_TTL_MILLIS = 180_000L
    }
}
