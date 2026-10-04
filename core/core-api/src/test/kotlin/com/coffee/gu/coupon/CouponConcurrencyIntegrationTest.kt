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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CouponConcurrencyIntegrationTest {

    @Autowired
    private lateinit var couponService: CouponService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private val principal = Principal("concurrent-user-01", PrincipalType.USER)
    private var testCouponId: Long = 0L

    @BeforeEach
    fun setUp() {
        jdbcTemplate.update("DELETE FROM issued_coupon")
        jdbcTemplate.update("DELETE FROM coupon")

        // 테스트용 쿠폰 1건 DB에 직접 생성 (ACTIVE 상태 필수)
        jdbcTemplate.update(
            """
            INSERT INTO coupon (name, type, discount, expired_at, entity_status, created_at, updated_at)
            VALUES ('선착순 1000원 할인 쿠폰', 'FIXED_AMOUNT', 1000, ?, 'ACTIVE', NOW(), NOW())
            """.trimIndent(),
            LocalDateTime.now().plusDays(7)
        )
        testCouponId = jdbcTemplate.queryForObject("SELECT id FROM coupon LIMIT 1", Long::class.java)!!
    }

    @Test
    @DisplayName("[동시성 회귀 테스트] 100개 스레드가 동일 유저로 동시 쿠폰 발급 요청 시, 락 및 트랜잭션 동기화로 인해 DB에는 정확히 1건만 발급되고 99건은 차단된다")
    fun test100ConcurrentCouponDownloadRequests() {
        val totalThreads = 100
        val executor = Executors.newFixedThreadPool(32)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(totalThreads)

        val successCount = AtomicInteger(0)
        val alreadyDownloadedCount = AtomicInteger(0)
        val lockTimeoutCount = AtomicInteger(0)
        val unexpectedErrorCount = AtomicInteger(0)
        val errors = java.util.concurrent.CopyOnWriteArrayList<Throwable>()

        for (i in 0 until totalThreads) {
            executor.submit {
                try {
                    startLatch.await() // 100개 스레드가 동시에 일제히 출발
                    couponService.download(principal, testCouponId)
                    successCount.incrementAndGet()
                } catch (e: CoreException) {
                    when (e.errorType) {
                        ErrorType.COUPON_ALREADY_DOWNLOADED -> alreadyDownloadedCount.incrementAndGet()
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

        // 전체 스레드 동시 출발
        startLatch.countDown()
        doneLatch.await()
        executor.shutdown()

        if (errors.isNotEmpty()) {
            println("=== [첫 번째 예상치 못한 에러] ===")
            errors.first().printStackTrace()
        }

        println("=== [쿠폰 100개 동시 요청 실측 결과] ===")
        println("성공(발급 완료): ${successCount.get()}")
        println("차단(이미 발급됨): ${alreadyDownloadedCount.get()}")
        println("차단(락 획득 타임아웃): ${lockTimeoutCount.get()}")
        println("예상치 못한 에러: ${unexpectedErrorCount.get()}")

        // 1. 비즈니스 로직 레벨 검증: 성공은 단 1회여야 함
        assertThat(successCount.get()).isEqualTo(1)
        assertThat(unexpectedErrorCount.get()).isEqualTo(0)
        assertThat(alreadyDownloadedCount.get() + lockTimeoutCount.get()).isEqualTo(totalThreads - 1)

        // 2. 실제 데이터베이스 영속성 레벨 검증: issued_coupon 테이블에 정확히 1개 레코드만 저장되어야 함
        val actualDbIssuedCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM issued_coupon WHERE principal_key = ? AND coupon_id = ?",
            Int::class.java,
            principal.key,
            testCouponId
        )
        assertThat(actualDbIssuedCount).isEqualTo(1)
    }
}
