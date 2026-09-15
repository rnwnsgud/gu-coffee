package com.coffee.gu.payment

import com.coffee.gu.Principal
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.enums.PaymentState
import jakarta.persistence.EntityManager
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionDefinition
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ShedLockVsSkipLockBenchmarkTest {

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var lockProvider: LockProvider

    @BeforeEach
    fun setUp() {
        lockProvider = JdbcTemplateLockProvider(jdbcTemplate)
    }

    private fun createPendingPayments(count: Int, prefix: String) {
        val status = transactionManager.getTransaction(DefaultTransactionDefinition())
        for (i in 0 until count) {
            val p = Payment(
                id = 0L,
                principal = Principal.user("USER-$prefix-$i"),
                orderKey = "ORDER-BENCH-$prefix-$i",
                originalAmount = BigDecimal.valueOf(10000),
                issuedCouponId = null,
                couponDiscount = null,
                amount = BigDecimal.valueOf(10000),
                state = PaymentState.PENDING_PG,
                externalPaymentKey = "PAY-KEY-BENCH-$prefix-$i",
                method = PaymentMethod.CARD,
                paidAt = null,
                approveCode = null,
                createdAt = LocalDateTime.now().minusMinutes(10),
                retryCount = 0
            )
            paymentRepository.save(p)
        }
        transactionManager.commit(status)
        jdbcTemplate.update("UPDATE payment SET updated_at = ?", Timestamp.valueOf(LocalDateTime.now().minusMinutes(10)))
    }

    @Test
    @DisplayName("ShedLock 방식: 대용량 건수(50건, 100건) 누적 시 1개 서버만 일하고(Single Worker Bottleneck) 타 서버는 Idle 0건 처리")
    fun benchmarkShedLockSingleWorkerBottleneck() {
        val recordCount = 50
        val prefix = UUID.randomUUID().toString().substring(0, 8)
        createPendingPayments(recordCount, prefix)

        val executor = Executors.newFixedThreadPool(2)
        val startLatch = CountDownLatch(1)
        val endLatch = CountDownLatch(2)

        val worker1ProcessedCount = AtomicInteger(0)
        val worker2ProcessedCount = AtomicInteger(0)

        val lockConfig = LockConfiguration(
            Instant.now(),
            "paymentRecoveryJob",
            Duration.ofSeconds(30),
            Duration.ofSeconds(5)
        )

        val startTime = System.currentTimeMillis()

        // Worker 1
        executor.submit {
            try {
                startLatch.await()
                val lock = lockProvider.lock(lockConfig)
                if (lock.isPresent) {
                    try {
                        while (true) {
                            val status = transactionManager.getTransaction(DefaultTransactionDefinition())
                            val pending = paymentRepository.claimPendingPayments(20)
                            if (pending.isEmpty()) {
                                transactionManager.commit(status)
                                break
                            }
                            for (p in pending) {
                                Thread.sleep(2)
                                jdbcTemplate.update("UPDATE payment SET state = 'SUCCESS' WHERE id = ?", p.id)
                                worker1ProcessedCount.incrementAndGet()
                            }
                            transactionManager.commit(status)
                        }
                    } finally {
                        lock.get().unlock()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                endLatch.countDown()
            }
        }

        // Worker 2
        executor.submit {
            try {
                startLatch.await()
                val lock = lockProvider.lock(lockConfig)
                if (lock.isPresent) {
                    try {
                        while (true) {
                            val status = transactionManager.getTransaction(DefaultTransactionDefinition())
                            val pending = paymentRepository.claimPendingPayments(20)
                            if (pending.isEmpty()) {
                                transactionManager.commit(status)
                                break
                            }
                            for (p in pending) {
                                Thread.sleep(2)
                                jdbcTemplate.update("UPDATE payment SET state = 'SUCCESS' WHERE id = ?", p.id)
                                worker2ProcessedCount.incrementAndGet()
                            }
                            transactionManager.commit(status)
                        }
                    } finally {
                        lock.get().unlock()
                    }
                }
            } catch (ignored: Exception) {
            } finally {
                endLatch.countDown()
            }
        }

        startLatch.countDown()
        endLatch.await()
        executor.shutdown()

        val elapsedTime = System.currentTimeMillis() - startTime

        println("[BENCHMARK - ShedLock] totalTime=${elapsedTime}ms, Worker1=${worker1ProcessedCount.get()}, Worker2=${worker2ProcessedCount.get()} (Idle)")

        assertThat(worker1ProcessedCount.get() + worker2ProcessedCount.get()).isEqualTo(50)
        assertThat(minOf(worker1ProcessedCount.get(), worker2ProcessedCount.get())).isEqualTo(0)
    }

    @Test
    @DisplayName("DB SKIP LOCKED 방식: 50건 대용량 건수에 대해 2개 서버가 동시에 레코드를 N분할로 병렬 분산 처리(Scale-Out)한다")
    fun benchmarkSkipLockParallelWorkers() {
        val recordCount = 50
        val prefix = UUID.randomUUID().toString().substring(0, 8)
        createPendingPayments(recordCount, prefix)

        val executor = Executors.newFixedThreadPool(2)
        val startLatch = CountDownLatch(1)
        val endLatch = CountDownLatch(2)

        val worker1ProcessedCount = AtomicInteger(0)
        val worker2ProcessedCount = AtomicInteger(0)

        val startTime = System.currentTimeMillis()

        // Worker 1
        executor.submit {
            try {
                startLatch.await()
                while (true) {
                    val status = transactionManager.getTransaction(DefaultTransactionDefinition())
                    val pending = paymentRepository.claimPendingPayments(20)
                    if (pending.isEmpty()) {
                        transactionManager.commit(status)
                        break
                    }
                    for (p in pending) {
                        Thread.sleep(2)
                        jdbcTemplate.update("UPDATE payment SET state = 'SUCCESS' WHERE id = ?", p.id)
                        worker1ProcessedCount.incrementAndGet()
                    }
                    transactionManager.commit(status)
                }
            } catch (ignored: Exception) {
            } finally {
                endLatch.countDown()
            }
        }

        // Worker 2
        executor.submit {
            try {
                startLatch.await()
                while (true) {
                    val status = transactionManager.getTransaction(DefaultTransactionDefinition())
                    val pending = paymentRepository.claimPendingPayments(20)
                    if (pending.isEmpty()) {
                        transactionManager.commit(status)
                        break
                    }
                    for (p in pending) {
                        Thread.sleep(2)
                        jdbcTemplate.update("UPDATE payment SET state = 'SUCCESS' WHERE id = ?", p.id)
                        worker2ProcessedCount.incrementAndGet()
                    }
                    transactionManager.commit(status)
                }
            } catch (ignored: Exception) {
            } finally {
                endLatch.countDown()
            }
        }

        startLatch.countDown()
        endLatch.await()
        executor.shutdown()

        val elapsedTime = System.currentTimeMillis() - startTime

        println("[BENCHMARK - SKIP LOCKED] totalTime=${elapsedTime}ms, Worker1=${worker1ProcessedCount.get()}, Worker2=${worker2ProcessedCount.get()} (Parallel)")

        assertThat(worker1ProcessedCount.get() + worker2ProcessedCount.get()).isEqualTo(50)
        assertThat(worker1ProcessedCount.get()).isGreaterThan(0)
        assertThat(worker2ProcessedCount.get()).isGreaterThan(0)
    }
}
