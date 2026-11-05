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

class RedisLimitedCouponClaimerTest {

    private val redissonClient: RedissonClient = mock()
    private val rScript: RScript = mock()
    private val rBucket: RBucket<String> = mock()
    private val claimer = RedisLimitedCouponClaimer(redissonClient)

    @Test
    @DisplayName("Lua Script 결과가 1이면 ClaimResult.SUCCESS를 반환한다")
    fun testClaimSuccess() {
        given(redissonClient.getScript(StringCodec.INSTANCE)).willReturn(rScript)
        given(
            rScript.eval<Long>(
                eq(RScript.Mode.READ_WRITE),
                any<String>(),
                eq(RScript.ReturnType.INTEGER),
                eq(listOf("coupon:limited:users:1", "coupon:limited:stock:1")),
                eq("user-1"),
            )
        ).willReturn(1L)

        val result = claimer.claim(1L, "user-1")

        assertThat(result).isEqualTo(LimitedCouponClaimer.ClaimResult.SUCCESS)
    }

    @Test
    @DisplayName("Lua Script 결과가 0이면 ClaimResult.SOLD_OUT을 반환한다")
    fun testClaimSoldOut() {
        given(redissonClient.getScript(StringCodec.INSTANCE)).willReturn(rScript)
        given(
            rScript.eval<Long>(
                eq(RScript.Mode.READ_WRITE),
                any<String>(),
                eq(RScript.ReturnType.INTEGER),
                eq(listOf("coupon:limited:users:1", "coupon:limited:stock:1")),
                eq("user-2"),
            )
        ).willReturn(0L)

        val result = claimer.claim(1L, "user-2")

        assertThat(result).isEqualTo(LimitedCouponClaimer.ClaimResult.SOLD_OUT)
    }

    @Test
    @DisplayName("Lua Script 결과가 -1이면 ClaimResult.ALREADY_CLAIMED를 반환한다")
    fun testClaimAlreadyClaimed() {
        given(redissonClient.getScript(StringCodec.INSTANCE)).willReturn(rScript)
        given(
            rScript.eval<Long>(
                eq(RScript.Mode.READ_WRITE),
                any<String>(),
                eq(RScript.ReturnType.INTEGER),
                eq(listOf("coupon:limited:users:1", "coupon:limited:stock:1")),
                eq("user-1"),
            )
        ).willReturn(-1L)

        val result = claimer.claim(1L, "user-1")

        assertThat(result).isEqualTo(LimitedCouponClaimer.ClaimResult.ALREADY_CLAIMED)
    }

    @Test
    @DisplayName("재고 키가 없을 때(-2) stockSupplier를 통해 재고를 초기화하고 재시도한다")
    fun testLazyStockInitialization() {
        given(redissonClient.getScript(StringCodec.INSTANCE)).willReturn(rScript)
        given(redissonClient.getBucket<String>(eq("coupon:limited:stock:1"), eq(StringCodec.INSTANCE))).willReturn(rBucket)

        // 첫 번째 호출은 NOT_INITIALIZED(-2), 재시도 호출은 SUCCESS(1)
        given(
            rScript.eval<Long>(
                eq(RScript.Mode.READ_WRITE),
                any<String>(),
                eq(RScript.ReturnType.INTEGER),
                eq(listOf("coupon:limited:users:1", "coupon:limited:stock:1")),
                eq("user-1"),
            )
        ).willReturn(-2L, 1L)

        val result = claimer.claim(1L, "user-1") { 50 }

        verify(rBucket).setIfAbsent("50")
        assertThat(result).isEqualTo(LimitedCouponClaimer.ClaimResult.SUCCESS)
    }
}
