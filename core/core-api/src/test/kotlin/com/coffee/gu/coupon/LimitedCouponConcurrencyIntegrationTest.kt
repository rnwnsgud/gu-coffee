package com.coffee.gu.coupon

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.enums.PrincipalType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import java.time.LocalDateTime
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class LimitedCouponConcurrencyIntegrationTest {

    @Autowired
    private lateinit var limitedCouponService: LimitedCouponService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private var testLimitedCouponId: Long = 0L

    @BeforeEach
    fun setUp() {
        jdbcTemplate.update("DELETE FROM issued_coupon")
        jdbcTemplate.update("DELETE FROM limited_coupon")
        jdbcTemplate.update("DELETE FROM coupon")

        // 1. 기반 Coupon 데이터 생성
        jdbcTemplate.update(
            """
            INSERT INTO coupon (name, type, discount, expired_at, entity_status, created_at, updated_at)
            VALUES ('오픈 기념 선착순 30명 3000원 할인 쿠폰', 'FIXED_AMOUNT', 3000, ?, 'ACTIVE', NOW(), NOW())
            """.trimIndent(),
            LocalDateTime.now().plusDays(7)
        )
        val baseCouponId = jdbcTemplate.queryForObject("SELECT id FROM coupon LIMIT 1", Long::class.java)!!

        // 2. 선착순 한정 수량 쿠폰 생성 (총 30개 수량, coupon_id 참조)
        jdbcTemplate.update(
            """
            INSERT INTO limited_coupon (coupon_id, total_quantity, issued_quantity, entity_status, created_at, updated_at)
            VALUES (?, 30, 0, 'ACTIVE', NOW(), NOW())
            """.trimIndent(),
            baseCouponId
        )
        testLimitedCouponId = jdbcTemplate.queryForObject("SELECT id FROM limited_coupon LIMIT 1", Long::class.java)!!
    }

    @Test
    @DisplayName("[동시성 통합 테스트] 100명의 서로 다른 유저가 30개 한정 수량 쿠폰을 동시 발급 요청 시, 분산 락 보호 하에 정확히 30개만 발급되고 오버이슈가 발생하지 않는다")
    fun test100ConcurrentUsersClaimLimitedCoupon30Quantity() {
        val totalThreads = 100
        val executor = Executors.newFixedThreadPool(32)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(totalThreads)

        val successCount = AtomicInteger(0)
        val soldOutCount = AtomicInteger(0)
        val lockTimeoutCount = AtomicInteger(0)
        val unexpectedErrorCount = AtomicInteger(0)
        val errors = CopyOnWriteArrayList<Throwable>()

        for (i in 1..totalThreads) {
            val user = Principal("concurrency-user-$i", PrincipalType.USER)
            executor.submit {
                try {
                    startLatch.await()
                    limitedCouponService.issue(user, testLimitedCouponId)
                    successCount.incrementAndGet()
                } catch (e: CoreException) {
                    when (e.errorType) {
                        ErrorType.LIMITED_COUPON_SOLD_OUT -> soldOutCount.incrementAndGet()
                        ErrorType.COUPON_LOCK_ACQUISITION_FAILED -> lockTimeoutCount.incrementAndGet()
                        else -> {
                            unexpectedErrorCount.incrementAndGet()
                            errors.add(e)
                        }
                    }
                } catch (e: Throwable) {
                    unexpectedErrorCount.incrementAndGet()
                    errors.add(e)
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        doneLatch.await()
        executor.shutdown()

        if (errors.isNotEmpty()) {
            println("=== [예상치 못한 에러] ===")
            errors.first().printStackTrace()
        }

        println("=== [선착순 쿠폰 100명 동시 요청 실측 결과] ===")
        println("성공(발급 완료): ${successCount.get()}")
        println("소진(SOLD_OUT 차단): ${soldOutCount.get()}")
        println("차단(락 획득 타임아웃): ${lockTimeoutCount.get()}")
        println("예상치 못한 에러: ${unexpectedErrorCount.get()}")

        // 1. 비즈니스 검증: 에러 없이 완료되고 정확히 재고 수량(30개)만 성공
        assertThat(unexpectedErrorCount.get()).isEqualTo(0)
        assertThat(successCount.get()).isEqualTo(30)
        assertThat(soldOutCount.get() + lockTimeoutCount.get()).isEqualTo(70)

        // 2. DB 영속성 검증: limited_coupon의 issued_quantity는 정확히 30
        val actualIssuedQuantity = jdbcTemplate.queryForObject(
            "SELECT issued_quantity FROM limited_coupon WHERE id = ?",
            Int::class.java,
            testLimitedCouponId
        )
        assertThat(actualIssuedQuantity).isEqualTo(30)

        // 3. DB 영속성 검증: issued_coupon 테이블에 저장된 발급 쿠폰 수도 정확히 30개
        val actualDbIssuedCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM issued_coupon WHERE coupon_id = ?",
            Int::class.java,
            testLimitedCouponId
        )
        assertThat(actualDbIssuedCount).isEqualTo(30)
    }

    @Test
    @DisplayName("[동시성 1인 1매 테스트] 동일 유저가 선착순 쿠폰을 10번 동시 요청해도 오직 1건만 발급되고 9건은 COUPON_ALREADY_DOWNLOADED로 차단된다")
    fun testSameUserConcurrentRequestsOnlyOneSuccess() {
        val sameUser = Principal("same-user-01", PrincipalType.USER)
        val totalThreads = 10
        val executor = Executors.newFixedThreadPool(totalThreads)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(totalThreads)

        val successCount = AtomicInteger(0)
        val alreadyDownloadedCount = AtomicInteger(0)
        val lockTimeoutCount = AtomicInteger(0)
        val unexpectedErrorCount = AtomicInteger(0)

        for (i in 1..totalThreads) {
            executor.submit {
                try {
                    startLatch.await()
                    limitedCouponService.issue(sameUser, testLimitedCouponId)
                    successCount.incrementAndGet()
                } catch (e: CoreException) {
                    when (e.errorType) {
                        ErrorType.COUPON_ALREADY_DOWNLOADED -> alreadyDownloadedCount.incrementAndGet()
                        ErrorType.COUPON_LOCK_ACQUISITION_FAILED -> lockTimeoutCount.incrementAndGet()
                        else -> unexpectedErrorCount.incrementAndGet()
                    }
                } catch (e: Throwable) {
                    unexpectedErrorCount.incrementAndGet()
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        doneLatch.await()
        executor.shutdown()

        assertThat(unexpectedErrorCount.get()).isEqualTo(0)
        assertThat(successCount.get()).isEqualTo(1)
        assertThat(alreadyDownloadedCount.get() + lockTimeoutCount.get()).isEqualTo(9)

        val actualDbIssuedCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM issued_coupon WHERE principal_key = ? AND coupon_id = ?",
            Int::class.java,
            sameUser.key,
            testLimitedCouponId
        )
        assertThat(actualDbIssuedCount).isEqualTo(1)
    }
}
