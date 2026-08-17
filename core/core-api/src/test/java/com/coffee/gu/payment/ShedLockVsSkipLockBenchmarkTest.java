package com.coffee.gu.payment;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.PaymentMethod;
import com.coffee.gu.enums.PaymentState;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ShedLockVsSkipLockBenchmarkTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private LockProvider lockProvider;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @BeforeEach
    void setUp() {
        lockProvider = new JdbcTemplateLockProvider(jdbcTemplate);
    }

    private void createPendingPayments(int count, String prefix) {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        for (int i = 0; i < count; i++) {
            Payment p = new Payment(
                    null,
                    Principal.user("USER-" + prefix + "-" + i),
                    "ORDER-BENCH-" + prefix + "-" + i,
                    BigDecimal.valueOf(10000),
                    null,
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(10000),
                    PaymentState.PENDING_PG,
                    "PAY-KEY-BENCH-" + prefix + "-" + i,
                    PaymentMethod.CARD,
                    null,
                    null,
                    LocalDateTime.now().minusMinutes(10),
                    0
            );
            paymentRepository.save(p);
        }
        transactionManager.commit(status);
        jdbcTemplate.update("UPDATE payment SET updated_at = ?", Timestamp.valueOf(LocalDateTime.now().minusMinutes(10)));
    }

    @Test
    @DisplayName("ShedLock 방식: 대용량 건수(50건, 100건) 누적 시 1개 서버만 일하고(Single Worker Bottleneck) 타 서버는 Idle 0건 처리")
    void benchmarkShedLockSingleWorkerBottleneck() throws Exception {
        int recordCount = 50;
        String prefix = UUID.randomUUID().toString().substring(0, 8);
        createPendingPayments(recordCount, prefix);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);

        AtomicInteger worker1ProcessedCount = new AtomicInteger(0);
        AtomicInteger worker2ProcessedCount = new AtomicInteger(0);

        LockConfiguration lockConfig = new LockConfiguration(
                Instant.now(),
                "paymentRecoveryJob",
                Duration.ofSeconds(30),
                Duration.ofSeconds(5)
        );

        long startTime = System.currentTimeMillis();

        // Worker 1
        executor.submit(() -> {
            try {
                startLatch.await();
                Optional<SimpleLock> lock = lockProvider.lock(lockConfig);
                if (lock.isPresent()) {
                    try {
                        while (true) {
                            TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
                            List<Payment> pending = paymentRepository.getPendingPayments(20);
                            if (pending.isEmpty()) {
                                transactionManager.commit(status);
                                break;
                            }
                            for (Payment p : pending) {
                                Thread.sleep(2);
                                jdbcTemplate.update("UPDATE payment SET state = 'SUCCESS' WHERE id = ?", p.getId());
                                worker1ProcessedCount.incrementAndGet();
                            }
                            transactionManager.commit(status);
                        }
                    } finally {
                        lock.get().unlock();
                    }
                }
            } catch (Exception ignored) {
            } finally {
                endLatch.countDown();
            }
        });

        // Worker 2
        executor.submit(() -> {
            try {
                startLatch.await();
                Optional<SimpleLock> lock = lockProvider.lock(lockConfig);
                if (lock.isPresent()) {
                    try {
                        while (true) {
                            TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
                            List<Payment> pending = paymentRepository.getPendingPayments(20);
                            if (pending.isEmpty()) {
                                transactionManager.commit(status);
                                break;
                            }
                            for (Payment p : pending) {
                                Thread.sleep(2);
                                jdbcTemplate.update("UPDATE payment SET state = 'SUCCESS' WHERE id = ?", p.getId());
                                worker2ProcessedCount.incrementAndGet();
                            }
                            transactionManager.commit(status);
                        }
                    } finally {
                        lock.get().unlock();
                    }
                }
            } catch (Exception ignored) {
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        long elapsedTime = System.currentTimeMillis() - startTime;

        System.out.println("[BENCHMARK - ShedLock] totalTime=" + elapsedTime + "ms, Worker1=" + worker1ProcessedCount.get() + ", Worker2=" + worker2ProcessedCount.get() + " (Idle)");

        // ShedLock 검증:
        // Worker 1 혼자서 50건을 전담 처리하며 (Single Worker Bottleneck), Worker 2는 락 선점 실패로 0건 처리 (Idle)
        assertThat(worker1ProcessedCount.get() + worker2ProcessedCount.get()).isEqualTo(50);
        assertThat(Math.min(worker1ProcessedCount.get(), worker2ProcessedCount.get())).isEqualTo(0);
    }

    @Test
    @DisplayName("DB SKIP LOCKED 방식: 50건 대용량 건수에 대해 2개 서버가 동시에 레코드를 N분할로 병렬 분산 처리(Scale-Out)한다")
    void benchmarkSkipLockParallelWorkers() throws Exception {
        int recordCount = 50;
        String prefix = UUID.randomUUID().toString().substring(0, 8);
        createPendingPayments(recordCount, prefix);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);

        AtomicInteger worker1ProcessedCount = new AtomicInteger(0);
        AtomicInteger worker2ProcessedCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // Worker 1
        executor.submit(() -> {
            try {
                startLatch.await();
                while (true) {
                    TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
                    List<Payment> pending = paymentRepository.getPendingPayments(20);
                    if (pending.isEmpty()) {
                        transactionManager.commit(status);
                        break;
                    }
                    for (Payment p : pending) {
                        Thread.sleep(2);
                        jdbcTemplate.update("UPDATE payment SET state = 'SUCCESS' WHERE id = ?", p.getId());
                        worker1ProcessedCount.incrementAndGet();
                    }
                    transactionManager.commit(status);
                }
            } catch (Exception ignored) {
            } finally {
                endLatch.countDown();
            }
        });

        // Worker 2
        executor.submit(() -> {
            try {
                startLatch.await();
                while (true) {
                    TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
                    List<Payment> pending = paymentRepository.getPendingPayments(20);
                    if (pending.isEmpty()) {
                        transactionManager.commit(status);
                        break;
                    }
                    for (Payment p : pending) {
                        Thread.sleep(2);
                        jdbcTemplate.update("UPDATE payment SET state = 'SUCCESS' WHERE id = ?", p.getId());
                        worker2ProcessedCount.incrementAndGet();
                    }
                    transactionManager.commit(status);
                }
            } catch (Exception ignored) {
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        long elapsedTime = System.currentTimeMillis() - startTime;

        System.out.println("[BENCHMARK - SKIP LOCKED] totalTime=" + elapsedTime + "ms, Worker1=" + worker1ProcessedCount.get() + ", Worker2=" + worker2ProcessedCount.get() + " (Parallel)");

        // SKIP LOCKED 검증:
        // 2개 워커가 레코드 락을 스킵하면서 50건 전체를 나누어 처리
        assertThat(worker1ProcessedCount.get() + worker2ProcessedCount.get()).isEqualTo(50);
        assertThat(worker1ProcessedCount.get()).isGreaterThan(0);
        assertThat(worker2ProcessedCount.get()).isGreaterThan(0);
    }
}
