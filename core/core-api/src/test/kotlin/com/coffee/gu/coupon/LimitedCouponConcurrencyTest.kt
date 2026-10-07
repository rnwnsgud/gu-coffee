package com.coffee.gu.coupon

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.enums.CouponType
import com.coffee.gu.enums.PrincipalType
import com.coffee.gu.lock.LockManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class LimitedCouponConcurrencyTest {

    private lateinit var limitedCouponFinder: LimitedCouponFinder
    private lateinit var limitedCouponIssueExecutor: LimitedCouponIssueExecutor
    private lateinit var lockManager: LockManager
    private lateinit var limitedCouponService: LimitedCouponService

    private val principal = Principal("user-123", PrincipalType.USER)
    private val limitedCouponId = 1L

    @BeforeEach
    fun setUp() {
        limitedCouponFinder = mock()
        limitedCouponIssueExecutor = mock()
        lockManager = mock()

        whenever(lockManager.executeWithLock<IssuedCoupon>(any(), any(), any(), any(), any())).thenAnswer { invocation ->
            val action = invocation.getArgument<() -> IssuedCoupon>(4)
            action()
        }

        limitedCouponService = LimitedCouponService(
            limitedCouponFinder = limitedCouponFinder,
            limitedCouponIssueExecutor = limitedCouponIssueExecutor,
            lockManager = lockManager
        )
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 호출 시 분산 락 키가 올바른 형식(LIMITED-COUPON-{couponId})으로 LockManager에 전달된다")
    fun testLimitedCouponLockKeyFormat() {
        val dummyCoupon = Coupon(
            id = limitedCouponId,
            name = "선착순 3000원 할인",
            type = CouponType.FIXED_AMOUNT,
            discount = BigDecimal.valueOf(3000),
            expiredAt = LocalDateTime.now().plusDays(7)
        )
        val dummyIssuedCoupon = IssuedCoupon(
            id = 100L,
            principal = principal,
            state = com.coffee.gu.enums.IssuedCouponState.DOWNLOADED,
            coupon = dummyCoupon
        )
        whenever(limitedCouponIssueExecutor.execute(principal, limitedCouponId)).thenReturn(dummyIssuedCoupon)

        val result = limitedCouponService.issue(principal, limitedCouponId)

        verify(lockManager).executeWithLock<IssuedCoupon>(
            eq("LIMITED-COUPON-1"),
            any(),
            any(),
            any(),
            any()
        )
        verify(limitedCouponIssueExecutor).execute(principal, limitedCouponId)
        assertThat(result.id).isEqualTo(100L)
    }

    @Test
    @DisplayName("총 수량이 30개인 선착순 쿠폰에 100개 요청이 인입될 때 정확히 30개만 발급되고 70개는 LIMITED_COUPON_SOLD_OUT으로 실패한다")
    fun testConcurrentIssueRespectsMaxQuantity() {
        val totalQuantity = 30
        val threadCount = 100
        val issuedCount = AtomicInteger(0)
        val soldOutCount = AtomicInteger(0)

        val dummyCoupon = Coupon(
            id = limitedCouponId,
            name = "선착순 3000원 할인",
            type = CouponType.FIXED_AMOUNT,
            discount = BigDecimal.valueOf(3000),
            expiredAt = LocalDateTime.now().plusDays(7)
        )

        doAnswer { invocation ->
            val current = issuedCount.incrementAndGet()
            if (current > totalQuantity) {
                throw CoreException(ErrorType.LIMITED_COUPON_SOLD_OUT)
            }
            IssuedCoupon(
                id = current.toLong(),
                principal = invocation.getArgument<Principal>(0),
                state = com.coffee.gu.enums.IssuedCouponState.DOWNLOADED,
                coupon = dummyCoupon
            )
        }.whenever(limitedCouponIssueExecutor).execute(any(), eq(limitedCouponId))

        val executor = Executors.newFixedThreadPool(32)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)

        for (i in 1..threadCount) {
            val user = Principal("user-$i", PrincipalType.USER)
            executor.submit {
                try {
                    startLatch.await()
                    limitedCouponService.issue(user, limitedCouponId)
                } catch (e: CoreException) {
                    if (e.errorType == ErrorType.LIMITED_COUPON_SOLD_OUT) {
                        soldOutCount.incrementAndGet()
                    }
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        doneLatch.await()
        executor.shutdown()

        assertThat(issuedCount.get()).isEqualTo(threadCount)
        assertThat(soldOutCount.get()).isEqualTo(threadCount - totalQuantity)
        verify(limitedCouponIssueExecutor, times(threadCount)).execute(any(), eq(limitedCouponId))
    }
}
