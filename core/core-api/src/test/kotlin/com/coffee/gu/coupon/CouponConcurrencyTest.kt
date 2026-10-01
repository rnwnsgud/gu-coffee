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

class CouponConcurrencyTest {

    private lateinit var couponFinder: CouponFinder
    private lateinit var couponDownloadExecutor: CouponDownloadExecutor
    private lateinit var lockManager: LockManager
    private lateinit var couponService: CouponService

    private val principal = Principal("user-123", PrincipalType.USER)
    private val couponId = 1L
    private val coupon = Coupon(
        id = couponId,
        name = "1000원 할인 쿠폰",
        type = CouponType.FIXED_AMOUNT,
        discount = BigDecimal.valueOf(1000),
        expiredAt = LocalDateTime.now().plusDays(7)
    )

    @BeforeEach
    fun setUp() {
        couponFinder = mock()
        couponDownloadExecutor = mock()
        lockManager = mock()

        whenever(couponFinder.getValidCoupon(couponId)).thenReturn(coupon)

        // mock LockManager to execute action directly or sequentially
        whenever(lockManager.executeWithLock<Unit>(any(), any(), any(), any(), any())).thenAnswer { invocation ->
            val action = invocation.getArgument<() -> Unit>(4)
            action()
        }

        couponService = CouponService(
            couponFinder = couponFinder,
            couponDownloadExecutor = couponDownloadExecutor,
            lockManager = lockManager
        )
    }

    @Test
    @DisplayName("쿠폰 다운로드 호출 시 분산 락 키가 올바른 형식(COUPON-DOWNLOAD-{couponId}-{principalKey})으로 LockManager에 전달된다")
    fun testCouponDownloadLockKeyFormat() {
        couponService.download(principal, couponId)

        verify(lockManager).executeWithLock<Unit>(
            eq("COUPON-DOWNLOAD-1-user-123"),
            any(),
            any(),
            any(),
            any()
        )
        verify(couponDownloadExecutor).download(principal, couponId)
    }

    @Test
    @DisplayName("동일 유저가 동시에 10번 쿠폰 발급을 요청해도 오직 1번만 성공하고 9번은 COUPON_ALREADY_DOWNLOADED로 차단된다")
    fun testConcurrentCouponDownloadOneSuccessOnly() {
        val issuedCount = AtomicInteger(0)
        val alreadyDownloadedCount = AtomicInteger(0)

        // 동기화 시뮬레이션: 첫 번째 스레드만 성공하고 이후 스레드는 DB 기등록 확인 후 예외 발생
        doAnswer {
            if (issuedCount.incrementAndGet() > 1) {
                throw CoreException(ErrorType.COUPON_ALREADY_DOWNLOADED)
            }
            null
        }.whenever(couponDownloadExecutor).download(principal, couponId)

        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)

        for (i in 0 until threadCount) {
            executor.submit {
                try {
                    startLatch.await()
                    couponService.download(principal, couponId)
                } catch (e: CoreException) {
                    if (e.errorType == ErrorType.COUPON_ALREADY_DOWNLOADED) {
                        alreadyDownloadedCount.incrementAndGet()
                    }
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        doneLatch.await()
        executor.shutdown()

        assertThat(issuedCount.get()).isEqualTo(10)
        assertThat(alreadyDownloadedCount.get()).isEqualTo(9)
        verify(couponDownloadExecutor, times(10)).download(principal, couponId)
    }
}
