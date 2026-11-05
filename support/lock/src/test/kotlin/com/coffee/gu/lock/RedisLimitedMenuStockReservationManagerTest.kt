package com.coffee.gu.lock

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.redisson.api.RBucket
import org.redisson.api.RScript
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec

class RedisLimitedMenuStockReservationManagerTest {

    private val redissonClient: RedissonClient = mock()
    private val rScript: RScript = mock()
    private val rBucket: RBucket<String> = mock()
    private val reservationManager = RedisLimitedMenuStockReservationManager(redissonClient)

    private val expectedKeys = listOf(
        "menu:limited:{10}:stock",
        "menu:limited:{10}:reservations:expiry",
        "menu:limited:{10}:reservations:qty",
        "menu:limited:{10}:reservations:user",
        "menu:limited:{10}:users:19700101",
    )

    @Test
    @DisplayName("keysFor가 생성하는 키의 순서와 해시 태그({menuId})는 Lua 스크립트 인덱스 계약과 일치해야 한다")
    fun keysOrderContract() {
        val menuId = 42L
        val keys = reservationManager.keysFor(menuId, 1000L)

        assertThat(keys).hasSize(5)
        // 0번(KEYS[1]): STRING (stock)
        assertThat(keys[RedisLimitedMenuStockReservationManager.KEY_INDEX_STOCK])
            .isEqualTo("menu:limited:{$menuId}:stock")
        // 1번(KEYS[2]): ZSET (expiry)
        assertThat(keys[RedisLimitedMenuStockReservationManager.KEY_INDEX_EXPIRY])
            .isEqualTo("menu:limited:{$menuId}:reservations:expiry")
        // 2번(KEYS[3]): HASH (qty)
        assertThat(keys[RedisLimitedMenuStockReservationManager.KEY_INDEX_QTY])
            .isEqualTo("menu:limited:{$menuId}:reservations:qty")
        // 3번(KEYS[4]): HASH (user)
        assertThat(keys[RedisLimitedMenuStockReservationManager.KEY_INDEX_USER])
            .isEqualTo("menu:limited:{$menuId}:reservations:user")
        // 4번(KEYS[5]): SET (daily purchased users partitioned by date)
        assertThat(keys[RedisLimitedMenuStockReservationManager.KEY_INDEX_USERS])
            .isEqualTo("menu:limited:{$menuId}:users:19700101")
    }

    @Test
    @DisplayName("Lua Script 결과가 1이면 ReservationResult.RESERVED를 반환한다")
    fun reserveSuccess() {
        given(redissonClient.getScript(StringCodec.INSTANCE)).willReturn(rScript)
        given(
            rScript.eval<Long>(
                eq(RScript.Mode.READ_WRITE),
                any<String>(),
                eq(RScript.ReturnType.INTEGER),
                eq(expectedKeys),
                eq("ORDER-1"),
                eq("user-1"),
                eq("1"),
                eq("1000"),
                eq("181000"),
            )
        ).willReturn(1L)

        val result = reservationManager.reserve(
            menuId = 10L,
            orderKey = "ORDER-1",
            principalKey = "user-1",
            quantity = 1,
            ttlMillis = 180_000L,
            nowEpochMillis = 1000L,
        )

        assertThat(result).isEqualTo(LimitedMenuStockReservationManager.ReservationResult.RESERVED)
    }

    @Test
    @DisplayName("Lua Script 결과가 0이면 ReservationResult.SOLD_OUT을 반환한다")
    fun reserveSoldOut() {
        given(redissonClient.getScript(StringCodec.INSTANCE)).willReturn(rScript)
        given(
            rScript.eval<Long>(
                eq(RScript.Mode.READ_WRITE),
                any<String>(),
                eq(RScript.ReturnType.INTEGER),
                eq(expectedKeys),
                eq("ORDER-2"),
                eq("user-2"),
                eq("1"),
                eq("1000"),
                eq("181000"),
            )
        ).willReturn(0L)

        val result = reservationManager.reserve(
            menuId = 10L,
            orderKey = "ORDER-2",
            principalKey = "user-2",
            quantity = 1,
            ttlMillis = 180_000L,
            nowEpochMillis = 1000L,
        )

        assertThat(result).isEqualTo(LimitedMenuStockReservationManager.ReservationResult.SOLD_OUT)
    }

    @Test
    @DisplayName("Lua Script 결과가 -1이면 ReservationResult.ALREADY_PURCHASED를 반환한다")
    fun reserveAlreadyPurchased() {
        given(redissonClient.getScript(StringCodec.INSTANCE)).willReturn(rScript)
        given(
            rScript.eval<Long>(
                eq(RScript.Mode.READ_WRITE),
                any<String>(),
                eq(RScript.ReturnType.INTEGER),
                eq(expectedKeys),
                eq("ORDER-3"),
                eq("user-1"),
                eq("1"),
                eq("1000"),
                eq("181000"),
            )
        ).willReturn(-1L)

        val result = reservationManager.reserve(
            menuId = 10L,
            orderKey = "ORDER-3",
            principalKey = "user-1",
            quantity = 1,
            ttlMillis = 180_000L,
            nowEpochMillis = 1000L,
        )

        assertThat(result).isEqualTo(LimitedMenuStockReservationManager.ReservationResult.ALREADY_PURCHASED)
    }

    @Test
    @DisplayName("재고 키가 없을 때(-2) stockSupplier로 지연 초기화하고 재시도한다")
    fun reserveLazyStockInitialization() {
        given(redissonClient.getScript(StringCodec.INSTANCE)).willReturn(rScript)
        given(redissonClient.getBucket<String>(eq("menu:limited:{10}:stock"), eq(StringCodec.INSTANCE))).willReturn(rBucket)
        given(
            rScript.eval<Long>(
                eq(RScript.Mode.READ_WRITE),
                any<String>(),
                eq(RScript.ReturnType.INTEGER),
                eq(expectedKeys),
                eq("ORDER-1"),
                eq("user-1"),
                eq("1"),
                eq("1000"),
                eq("181000"),
            )
        ).willReturn(-2L, 1L)

        val result = reservationManager.reserve(
            menuId = 10L,
            orderKey = "ORDER-1",
            principalKey = "user-1",
            quantity = 1,
            ttlMillis = 180_000L,
            nowEpochMillis = 1000L,
        ) { 30 }

        verify(rBucket).setIfAbsent("30")
        assertThat(result).isEqualTo(LimitedMenuStockReservationManager.ReservationResult.RESERVED)
    }

    @Test
    @DisplayName("verifyAndConfirm 호출 시 만료되지 않았으면(1) CONFIRMED, 만료되었으면(0) EXPIRED를 반환한다")
    fun verifyAndConfirmTest() {
        given(redissonClient.getScript(StringCodec.INSTANCE)).willReturn(rScript)
        given(
            rScript.eval<Long>(
                eq(RScript.Mode.READ_WRITE),
                any<String>(),
                eq(RScript.ReturnType.INTEGER),
                eq(expectedKeys),
                eq("ORDER-1"),
                eq("50000"),
            )
        ).willReturn(1L, 0L)

        val first = reservationManager.verifyAndConfirm(10L, "ORDER-1", 50000L)
        val second = reservationManager.verifyAndConfirm(10L, "ORDER-1", 50000L)

        assertThat(first).isEqualTo(LimitedMenuStockReservationManager.ConfirmationResult.CONFIRMED)
        assertThat(second).isEqualTo(LimitedMenuStockReservationManager.ConfirmationResult.EXPIRED)
    }
}
