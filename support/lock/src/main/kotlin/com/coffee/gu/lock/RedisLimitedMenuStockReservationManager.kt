package com.coffee.gu.lock

import com.coffee.gu.lock.LimitedMenuStockReservationManager.ConfirmationResult
import com.coffee.gu.lock.LimitedMenuStockReservationManager.ReservationResult
import org.redisson.api.RScript
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.springframework.core.io.ClassPathResource

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class RedisLimitedMenuStockReservationManager(
    private val redissonClient: RedissonClient,
) : LimitedMenuStockReservationManager {

    override fun reserve(
        menuId: Long,
        orderKey: String,
        principalKey: String,
        quantity: Int,
        ttlMillis: Long,
        nowEpochMillis: Long,
        stockSupplier: (() -> Int)?,
    ): ReservationResult {
        val keys = keysFor(menuId, nowEpochMillis)
        val expireEpochMillis = nowEpochMillis + ttlMillis

        var result = executeReserveScript(keys, orderKey, principalKey, quantity, nowEpochMillis, expireEpochMillis)

        if (result == NOT_INITIALIZED) {
            val initialStock = stockSupplier?.invoke() ?: 0
            val stockBucket = redissonClient.getBucket<String>(keys[0], StringCodec.INSTANCE)
            stockBucket.setIfAbsent(initialStock.toString())
            result = executeReserveScript(keys, orderKey, principalKey, quantity, nowEpochMillis, expireEpochMillis)
        }

        return when (result) {
            1L -> ReservationResult.RESERVED
            0L -> ReservationResult.SOLD_OUT
            -1L -> ReservationResult.ALREADY_PURCHASED
            else -> ReservationResult.SOLD_OUT
        }
    }

    private fun executeReserveScript(
        keys: List<String>,
        orderKey: String,
        principalKey: String,
        quantity: Int,
        nowEpochMillis: Long,
        expireEpochMillis: Long,
    ): Long {
        val script = redissonClient.getScript(StringCodec.INSTANCE)
        val res = script.eval<Long>(
            RScript.Mode.READ_WRITE,
            RESERVE_LUA_SCRIPT,
            RScript.ReturnType.INTEGER,
            keys,
            orderKey,
            principalKey,
            quantity.toString(),
            nowEpochMillis.toString(),
            expireEpochMillis.toString(),
        )
        return res ?: 0L
    }

    override fun verifyAndConfirm(
        menuId: Long,
        orderKey: String,
        nowEpochMillis: Long,
    ): ConfirmationResult {
        val keys = keysFor(menuId, nowEpochMillis)
        val script = redissonClient.getScript(StringCodec.INSTANCE)
        val result = script.eval<Long>(
            RScript.Mode.READ_WRITE,
            VERIFY_AND_CONFIRM_LUA_SCRIPT,
            RScript.ReturnType.INTEGER,
            keys,
            orderKey,
            nowEpochMillis.toString(),
        ) ?: 0L

        return if (result == 1L) {
            ConfirmationResult.CONFIRMED
        } else {
            ConfirmationResult.EXPIRED
        }
    }

    override fun release(
        menuId: Long,
        orderKey: String,
        principalKey: String,
        quantity: Int,
        nowEpochMillis: Long,
    ) {
        val keys = keysFor(menuId, nowEpochMillis)
        val script = redissonClient.getScript(StringCodec.INSTANCE)
        script.eval<Long>(
            RScript.Mode.READ_WRITE,
            RELEASE_LUA_SCRIPT,
            RScript.ReturnType.INTEGER,
            keys,
            orderKey,
            principalKey,
            quantity.toString(),
        )
    }

    override fun initializeStock(menuId: Long, totalQuantity: Int) {
        val keys = keysFor(menuId)
        redissonClient.keys.delete(*keys.toTypedArray())
        val stockBucket = redissonClient.getBucket<String>(keys[0], StringCodec.INSTANCE)
        stockBucket.set(totalQuantity.toString())
    }

    override fun getAvailableStock(menuId: Long, nowEpochMillis: Long): Int {
        val keys = keysFor(menuId, nowEpochMillis)
        val script = redissonClient.getScript(StringCodec.INSTANCE)
        val available = script.eval<Long>(
            RScript.Mode.READ_WRITE,
            AVAILABLE_STOCK_LUA_SCRIPT,
            RScript.ReturnType.INTEGER,
            keys,
            nowEpochMillis.toString(),
        ) ?: 0L
        return available.toInt().coerceAtLeast(0)
    }

    internal fun keysFor(menuId: Long, nowEpochMillis: Long = System.currentTimeMillis()): List<String> {
        val dateString = formatDate(nowEpochMillis)
        return listOf(
            "menu:limited:{$menuId}:stock",
            "menu:limited:{$menuId}:reservations:expiry",
            "menu:limited:{$menuId}:reservations:qty",
            "menu:limited:{$menuId}:reservations:user",
            "menu:limited:{$menuId}:users:$dateString",
        )
    }

    private fun formatDate(nowEpochMillis: Long): String {
        return DATE_FORMATTER
            .withZone(SEOUL_ZONE_ID)
            .format(Instant.ofEpochMilli(nowEpochMillis))
    }

    companion object {
        private val SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul")
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
        const val KEY_INDEX_STOCK = 0        // KEYS[1] in Lua: STRING (가용 재고)
        const val KEY_INDEX_EXPIRY = 1       // KEYS[2] in Lua: ZSET (만료 시각 정렬 집합)
        const val KEY_INDEX_QTY = 2          // KEYS[3] in Lua: HASH (주문별 선점 수량)
        const val KEY_INDEX_USER = 3         // KEYS[4] in Lua: HASH (주문별 유저 식별자)
        const val KEY_INDEX_USERS = 4        // KEYS[5] in Lua: SET (당일 구매 유저 집합)
        const val KEY_INDEX_ACTIVE_USERS = KEY_INDEX_USERS

        private const val NOT_INITIALIZED = -2L

        private val RESERVE_LUA_SCRIPT = loadScript("scripts/reserve_stock.lua")
        private val VERIFY_AND_CONFIRM_LUA_SCRIPT = loadScript("scripts/verify_and_confirm_stock.lua")
        private val RELEASE_LUA_SCRIPT = loadScript("scripts/release_stock.lua")
        private val AVAILABLE_STOCK_LUA_SCRIPT = loadScript("scripts/available_stock.lua")

        private fun loadScript(path: String): String {
            return ClassPathResource(path).inputStream.bufferedReader().use { it.readText() }
        }
    }
}
