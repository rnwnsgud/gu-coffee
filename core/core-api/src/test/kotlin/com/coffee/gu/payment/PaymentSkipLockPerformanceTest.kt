package com.coffee.gu.payment

import com.coffee.gu.Principal
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.enums.PaymentState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionDefinition
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PaymentSkipLockPerformanceTest {

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Autowired
    private lateinit var jdbcTemplate: org.springframework.jdbc.core.JdbcTemplate

    @Test
    @DisplayName("SKIP LOCKED 도입 시 락 선점 중인 batchSize개 대용량 건에 대해 타 워커 스레드는 대기 시간 0ms에 가깝게 즉시 스킵(Non-blocking)한다")
    fun testSkipLockNonBlockingPerformance() {
        // given
        val batchSize = 50
        val prefix = UUID.randomUUID().toString().substring(0, 8)
        for (i in 0 until batchSize) {
            val p = Payment(
                id = 0L,
                principal = Principal.user("USER-$prefix-$i"),
                orderKey = "ORDER-PERF-$prefix-$i",
                originalAmount = BigDecimal.valueOf(10000),
                issuedCouponId = null,
                couponDiscount = null,
                amount = BigDecimal.valueOf(10000),
                state = PaymentState.PENDING_PG,
                externalPaymentKey = "PAY-KEY-PERF-$prefix-$i",
                method = PaymentMethod.CARD,
                paidAt = null,
                approveCode = null,
                createdAt = LocalDateTime.now().minusMinutes(10),
                retryCount = 0
            )
            paymentRepository.save(p)
        }
        jdbcTemplate.update("UPDATE payment SET updated_at = ?", java.sql.Timestamp.valueOf(LocalDateTime.now().minusMinutes(10)))

        val executor = Executors.newFixedThreadPool(2)
        val worker1LockAcquiredLatch = CountDownLatch(1)
        val worker2DoneLatch = CountDownLatch(1)

        // Worker 1: n개 PENDING_PG 건에 대해 FOR UPDATE SKIP LOCKED 락을 쥐고 대기
        val worker1Future = executor.submit<List<Payment>> {
            val status = transactionManager.getTransaction(DefaultTransactionDefinition())
            val lockedPayments = paymentRepository.claimPendingPayments(batchSize)
            worker1LockAcquiredLatch.countDown()

            worker2DoneLatch.await(5, TimeUnit.SECONDS)
            transactionManager.rollback(status)
            lockedPayments
        }

        worker1LockAcquiredLatch.await(3, TimeUnit.SECONDS)

        // Worker 2: 동시 진입하여 claimPendingPayments() 실행 시 락 대기 없이 즉시 Non-blocking 수행
        val startTime = System.currentTimeMillis()
        val worker2Future = executor.submit<List<Payment>> {
            val status = transactionManager.getTransaction(DefaultTransactionDefinition())
            val result = paymentRepository.claimPendingPayments(batchSize)
            transactionManager.commit(status)
            result
        }

        val worker2Result = worker2Future.get(3, TimeUnit.SECONDS)
        val elapsedTime = System.currentTimeMillis() - startTime

        worker2DoneLatch.countDown()
        val worker1Result = worker1Future.get()
        executor.shutdown()

        // then
        // 1. Worker 1은 50건을 정상 선점
        assertThat(worker1Result).hasSize(batchSize)

        // 2. Worker 1이 50개 레코드 락을 선점 중이지만, Worker 2는 Worker 1의 대기(최대 5초)에 블로킹되지 않고 즉시 완료됨 (Non-blocking)
        assertThat(elapsedTime).isLessThan(2000)

        // 3. Worker 1이 점유한 레코드는 스킵되어 Worker 2에게는 빈 리스트가 반환됨 (안전한 작업 분할)
        assertThat(worker2Result).isEmpty()
    }

    @Test
    @DisplayName("SKIP LOCKED 도입 시 100건의 대용량 건을 2개 워커가 동시 claim할 때 락 경합 없이 각각 50건씩 배타적으로 분할 선점한다")
    fun testSkipLockConcurrentPartitioning() {
        // given
        val totalCount = 100
        val batchSize = 50
        val prefix = UUID.randomUUID().toString().substring(0, 8)
        for (i in 0 until totalCount) {
            val p = Payment(
                id = 0L,
                principal = Principal.user("USER-PART-$prefix-$i"),
                orderKey = "ORDER-PART-$prefix-$i",
                originalAmount = BigDecimal.valueOf(10000),
                issuedCouponId = null,
                couponDiscount = null,
                amount = BigDecimal.valueOf(10000),
                state = PaymentState.PENDING_PG,
                externalPaymentKey = "PAY-KEY-PART-$prefix-$i",
                method = PaymentMethod.CARD,
                paidAt = null,
                approveCode = null,
                createdAt = LocalDateTime.now().minusMinutes(10),
                retryCount = 0
            )
            paymentRepository.save(p)
        }
        jdbcTemplate.update("UPDATE payment SET updated_at = ?", java.sql.Timestamp.valueOf(LocalDateTime.now().minusMinutes(10)))

        val executor = Executors.newFixedThreadPool(2)
        val startLatch = CountDownLatch(1)
        val worker1AcquiredLatch = CountDownLatch(1)

        // Worker 1: 50건 claim 후 락을 쥐고 대기
        val worker1Future = executor.submit<List<Long>> {
            val status = transactionManager.getTransaction(DefaultTransactionDefinition())
            startLatch.await()
            val locked = paymentRepository.claimPendingPayments(batchSize)
            worker1AcquiredLatch.countDown()
            Thread.sleep(300) // 락 점유 유지
            transactionManager.commit(status)
            locked.map { it.id }
        }

        // Worker 2: Worker 1이 락을 쥐고 있는 도중 진입하여 나머지 50건 claim
        val worker2Future = executor.submit<List<Long>> {
            startLatch.await()
            worker1AcquiredLatch.await() // Worker 1이 50건 락을 쥔 직후 실행
            val status = transactionManager.getTransaction(DefaultTransactionDefinition())
            val locked = paymentRepository.claimPendingPayments(batchSize)
            transactionManager.commit(status)
            locked.map { it.id }
        }

        startLatch.countDown()
        val worker1Ids = worker1Future.get()
        val worker2Ids = worker2Future.get()
        executor.shutdown()

        // then
        // 1. Worker 1과 Worker 2가 각각 50건씩 정확히 선점
        assertThat(worker1Ids).hasSize(50)
        assertThat(worker2Ids).hasSize(50)

        // 2. 두 워커 간 중복 선점(Lock Contention/Data Race)이 전혀 없음 (교집합 = 공집합)
        val intersection = worker1Ids.intersect(worker2Ids.toSet())
        assertThat(intersection).isEmpty()

        // 3. 전체 100건이 두 워커에게 안전하게 분할 할당됨
        assertThat(worker1Ids.size + worker2Ids.size).isEqualTo(totalCount)
    }
}
