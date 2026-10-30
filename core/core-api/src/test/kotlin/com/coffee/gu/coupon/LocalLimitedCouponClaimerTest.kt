package com.coffee.gu.coupon

import com.coffee.gu.lock.LimitedCouponClaimer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class LocalLimitedCouponClaimerTest {

    private val claimer = LocalLimitedCouponClaimer()

    @Test
    @DisplayName("동일 유저가 5번 연속 요청하면 1번만 SUCCESS이고 4번은 ALREADY_CLAIMED가 반환된다")
    fun testSameUserClaim() {
        claimer.initializeStock(1L, 10)

        val first = claimer.claim(1L, "user-1")
        assertThat(first).isEqualTo(LimitedCouponClaimer.ClaimResult.SUCCESS)

        for (i in 1..4) {
            val next = claimer.claim(1L, "user-1")
            assertThat(next).isEqualTo(LimitedCouponClaimer.ClaimResult.ALREADY_CLAIMED)
        }
    }

    @Test
    @DisplayName("동시 10개 요청 시 재고가 3개라면 정확히 3개만 SUCCESS이고 7개는 SOLD_OUT이 반환된다")
    fun testConcurrencyClaim() {
        claimer.initializeStock(2L, 3)

        val totalThreads = 10
        val executor = Executors.newFixedThreadPool(totalThreads)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(totalThreads)

        val successCount = AtomicInteger(0)
        val soldOutCount = AtomicInteger(0)

        for (i in 1..totalThreads) {
            val user = "concurrency-user-$i"
            executor.submit {
                try {
                    startLatch.await()
                    val result = claimer.claim(2L, user)
                    when (result) {
                        LimitedCouponClaimer.ClaimResult.SUCCESS -> successCount.incrementAndGet()
                        LimitedCouponClaimer.ClaimResult.SOLD_OUT -> soldOutCount.incrementAndGet()
                        else -> {}
                    }
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        doneLatch.await()
        executor.shutdown()

        assertThat(successCount.get()).isEqualTo(3)
        assertThat(soldOutCount.get()).isEqualTo(7)
        assertThat(claimer.getRemainingStock(2L)).isEqualTo(0)
    }
}
