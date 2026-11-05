package com.coffee.gu.menu

import com.coffee.gu.lock.LimitedMenuStockReservationManager
import com.coffee.gu.lock.LimitedMenuStockReservationManager.ConfirmationResult
import com.coffee.gu.lock.LimitedMenuStockReservationManager.ReservationResult
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

class LocalLimitedMenuStockReservationManager : LimitedMenuStockReservationManager {

    private data class ReservationEntry(
        val orderKey: String,
        val principalKey: String,
        val quantity: Int,
        var expireEpochMillis: Long,
        val dateString: String,
    )

    private val stocks = ConcurrentHashMap<Long, Int>()
    private val reservations = ConcurrentHashMap<Long, MutableMap<String, ReservationEntry>>()
    private val activeUsers = ConcurrentHashMap<Pair<Long, String>, MutableSet<String>>()

    private fun formatDate(nowEpochMillis: Long): String {
        return DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneId.of("Asia/Seoul"))
            .format(Instant.ofEpochMilli(nowEpochMillis))
    }

    @Synchronized
    override fun reserve(
        menuId: Long,
        orderKey: String,
        principalKey: String,
        quantity: Int,
        ttlMillis: Long,
        nowEpochMillis: Long,
        stockSupplier: (() -> Int)?,
    ): ReservationResult {
        if (!stocks.containsKey(menuId)) {
            stocks[menuId] = stockSupplier?.invoke() ?: 0
        }

        cleanupExpired(menuId, nowEpochMillis)

        val dateString = formatDate(nowEpochMillis)
        val menuReservations = reservations.computeIfAbsent(menuId) { LinkedHashMap() }
        val users = activeUsers.computeIfAbsent(menuId to dateString) { LinkedHashSet() }

        if (menuReservations.containsKey(orderKey)) {
            return ReservationResult.RESERVED
        }

        if (users.contains(principalKey)) {
            return ReservationResult.ALREADY_PURCHASED
        }

        val currentStock = stocks[menuId] ?: 0
        if (currentStock < quantity) {
            return ReservationResult.SOLD_OUT
        }

        stocks[menuId] = currentStock - quantity
        menuReservations[orderKey] = ReservationEntry(
            orderKey = orderKey,
            principalKey = principalKey,
            quantity = quantity,
            expireEpochMillis = nowEpochMillis + ttlMillis,
            dateString = dateString,
        )
        users.add(principalKey)
        return ReservationResult.RESERVED
    }

    @Synchronized
    override fun verifyAndConfirm(
        menuId: Long,
        orderKey: String,
        nowEpochMillis: Long,
    ): ConfirmationResult {
        val menuReservations = reservations[menuId] ?: return ConfirmationResult.EXPIRED
        val entry = menuReservations[orderKey] ?: return ConfirmationResult.EXPIRED

        if (entry.expireEpochMillis <= nowEpochMillis) {
            stocks[menuId] = (stocks[menuId] ?: 0) + entry.quantity
            menuReservations.remove(orderKey)
            activeUsers[menuId to entry.dateString]?.remove(entry.principalKey)
            return ConfirmationResult.EXPIRED
        }

        menuReservations.remove(orderKey)
        return ConfirmationResult.CONFIRMED
    }

    @Synchronized
    override fun release(
        menuId: Long,
        orderKey: String,
        principalKey: String,
        quantity: Int,
        nowEpochMillis: Long,
    ) {
        val menuReservations = reservations[menuId]
        val removedEntry = menuReservations?.remove(orderKey)
        if (removedEntry != null) {
            stocks[menuId] = (stocks[menuId] ?: 0) + removedEntry.quantity
            activeUsers[menuId to removedEntry.dateString]?.remove(removedEntry.principalKey)
            return
        }

        val dateString = formatDate(nowEpochMillis)
        val users = activeUsers[menuId to dateString]
        if (users != null && users.remove(principalKey)) {
            stocks[menuId] = (stocks[menuId] ?: 0) + quantity
        }
    }

    @Synchronized
    override fun initializeStock(menuId: Long, totalQuantity: Int) {
        stocks[menuId] = totalQuantity
        reservations.remove(menuId)
        activeUsers.clear()
    }

    @Synchronized
    override fun getAvailableStock(menuId: Long, nowEpochMillis: Long): Int {
        cleanupExpired(menuId, nowEpochMillis)
        return (stocks[menuId] ?: 0).coerceAtLeast(0)
    }

    @Synchronized
    fun forceExpireOrder(menuId: Long, orderKey: String) {
        reservations[menuId]?.get(orderKey)?.let {
            it.expireEpochMillis = 0L
        }
    }

    private fun cleanupExpired(menuId: Long, nowEpochMillis: Long) {
        val menuReservations = reservations[menuId] ?: return
        val iterator = menuReservations.entries.iterator()
        while (iterator.hasNext()) {
            val (_, entry) = iterator.next()
            if (entry.expireEpochMillis <= nowEpochMillis) {
                stocks[menuId] = (stocks[menuId] ?: 0) + entry.quantity
                activeUsers[menuId to entry.dateString]?.remove(entry.principalKey)
                iterator.remove()
            }
        }
    }
}
