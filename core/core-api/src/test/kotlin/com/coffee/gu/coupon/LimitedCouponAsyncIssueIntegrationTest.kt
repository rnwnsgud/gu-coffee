package com.coffee.gu.coupon

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.TestApplication
import com.coffee.gu.enums.PrincipalType
import com.coffee.gu.kafka.KafkaTopic
import com.coffee.gu.lock.LimitedCouponClaimer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(
    classes = [TestApplication::class],
    properties = ["spring.kafka.listener.auto-startup=true"],
)
@EmbeddedKafka(
    partitions = 3,
    topics = [KafkaTopic.LIMITED_COUPON_ISSUE_V1, KafkaTopic.LIMITED_COUPON_ISSUE_V1_DLT],
    bootstrapServersProperty = "spring.kafka.bootstrap-servers",
)
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class LimitedCouponAsyncIssueIntegrationTest {

    @Autowired
    private lateinit var limitedCouponService: LimitedCouponService

    @Autowired
    private lateinit var limitedCouponClaimer: LimitedCouponClaimer

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var registry: KafkaListenerEndpointRegistry

    @Autowired
    private lateinit var embeddedKafka: EmbeddedKafkaBroker

    private var testLimitedCouponId: Long = 0L

    @BeforeEach
    fun setUp() {
        registry.listenerContainers.forEach { container ->
            ContainerTestUtils.waitForAssignment(container, embeddedKafka.partitionsPerTopic)
        }

        jdbcTemplate.update("DELETE FROM issued_coupon")
        jdbcTemplate.update("DELETE FROM limited_coupon")
        jdbcTemplate.update("DELETE FROM coupon")

        jdbcTemplate.update(
            """
            INSERT INTO coupon (name, type, discount, expired_at, entity_status, created_at, updated_at)
            VALUES ('선착순 비동기 30명 쿠폰', 'FIXED_AMOUNT', 3000, ?, 'ACTIVE', NOW(), NOW())
            """.trimIndent(),
            LocalDateTime.now().plusDays(7),
        )
        val baseCouponId = jdbcTemplate.queryForObject("SELECT id FROM coupon LIMIT 1", Long::class.java)!!

        jdbcTemplate.update(
            """
            INSERT INTO limited_coupon (coupon_id, total_quantity, issued_quantity, entity_status, created_at, updated_at)
            VALUES (?, 30, 0, 'ACTIVE', NOW(), NOW())
            """.trimIndent(),
            baseCouponId,
        )
        testLimitedCouponId = jdbcTemplate.queryForObject("SELECT id FROM limited_coupon LIMIT 1", Long::class.java)!!

        // 인메모리 재고 초기화
        limitedCouponClaimer.initializeStock(testLimitedCouponId, 30)
    }

    @Test
    @DisplayName("[비동기 대기열 통합 테스트] 100명이 30개 한정 쿠폰 동시 요청 시, 30명만 ACCEPTED되고 70명은 즉시 SOLD_OUT으로 탈락하며 컨슈머를 통해 DB에는 정확히 30건만 영속화된다")
    fun test100ConcurrentUsersClaimLimitedCouponAsync30Quantity() {
        val totalThreads = 100
        val executor = Executors.newFixedThreadPool(32)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(totalThreads)

        val successCount = AtomicInteger(0)
        val soldOutCount = AtomicInteger(0)
        val unexpectedErrors = CopyOnWriteArrayList<Throwable>()

        for (i in 1..totalThreads) {
            val user = Principal("async-user-$i", PrincipalType.USER)
            executor.submit {
                try {
                    startLatch.await()
                    val receipt = limitedCouponService.issueAsync(user, testLimitedCouponId)
                    if (receipt.eventId.isNotBlank()) {
                        successCount.incrementAndGet()
                    }
                } catch (e: CoreException) {
                    if (e.errorType == ErrorType.LIMITED_COUPON_SOLD_OUT) {
                        soldOutCount.incrementAndGet()
                    } else {
                        unexpectedErrors.add(e)
                    }
                } catch (e: Throwable) {
                    unexpectedErrors.add(e)
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        doneLatch.await()
        executor.shutdown()

        // 1. 인메모리 사전 차단 검증: 정확히 30건 성공, 70건 즉시 소진 탈락
        assertThat(unexpectedErrors).isEmpty()
        assertThat(successCount.get()).isEqualTo(30)
        assertThat(soldOutCount.get()).isEqualTo(70)

        // 2. 비동기 Kafka 컨슈머가 DB에 영속화할 때까지 대기 (최대 10초 폴링)
        var actualDbCount = 0
        val maxWaitMs = 10000L
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            actualDbCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM issued_coupon WHERE coupon_id = ?",
                Int::class.java,
                testLimitedCouponId,
            ) ?: 0
            if (actualDbCount >= 30) break
            TimeUnit.MILLISECONDS.sleep(100)
        }

        // 3. 최종 DB 영속성 검증: 정확히 30개만 저장되고 오버이슈 0건
        assertThat(actualDbCount).isEqualTo(30)

        val actualIssuedQuantity = jdbcTemplate.queryForObject(
            "SELECT issued_quantity FROM limited_coupon WHERE id = ?",
            Int::class.java,
            testLimitedCouponId,
        )
        assertThat(actualIssuedQuantity).isEqualTo(30)
    }

    @Test
    @DisplayName("[비동기 1인 1매 테스트] 동일 유저가 10번 동시 비동기 발급 요청 시 오직 1건만 ACCEPTED되고 9건은 즉시 COUPON_ALREADY_DOWNLOADED로 차단된다")
    fun testSameUserConcurrentAsyncRequestsOnlyOneAccepted() {
        val sameUser = Principal("same-async-user-01", PrincipalType.USER)
        val totalThreads = 10
        val executor = Executors.newFixedThreadPool(totalThreads)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(totalThreads)

        val acceptedCount = AtomicInteger(0)
        val alreadyDownloadedCount = AtomicInteger(0)
        val unexpectedErrors = CopyOnWriteArrayList<Throwable>()

        for (i in 1..totalThreads) {
            executor.submit {
                try {
                    startLatch.await()
                    val receipt = limitedCouponService.issueAsync(sameUser, testLimitedCouponId)
                    if (receipt.eventId.isNotBlank()) {
                        acceptedCount.incrementAndGet()
                    }
                } catch (e: CoreException) {
                    if (e.errorType == ErrorType.COUPON_ALREADY_DOWNLOADED) {
                        alreadyDownloadedCount.incrementAndGet()
                    } else {
                        unexpectedErrors.add(e)
                    }
                } catch (e: Throwable) {
                    unexpectedErrors.add(e)
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        doneLatch.await()
        executor.shutdown()

        assertThat(unexpectedErrors).isEmpty()
        assertThat(acceptedCount.get()).isEqualTo(1)
        assertThat(alreadyDownloadedCount.get()).isEqualTo(9)

        // 비동기 처리 완료 대기
        var actualDbCount = 0
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 5000L) {
            actualDbCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM issued_coupon WHERE principal_key = ? AND coupon_id = ?",
                Int::class.java,
                sameUser.key,
                testLimitedCouponId,
            ) ?: 0
            if (actualDbCount >= 1) break
            TimeUnit.MILLISECONDS.sleep(100)
        }

        assertThat(actualDbCount).isEqualTo(1)
    }
}
