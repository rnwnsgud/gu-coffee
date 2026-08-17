package com.coffee.gu.payment;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.PaymentMethod;
import com.coffee.gu.enums.PaymentState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PaymentSkipLockPerformanceTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("SKIP LOCKED 도입 시 락 선점 중인 batchSize개 대용량 건에 대해 타 워커 스레드는 대기 시간 0ms에 가깝게 즉시 스킵(Non-blocking)한다")
    void testSkipLockNonBlockingPerformance() throws Exception {
        // given
        int batchSize = 50;
        String prefix = UUID.randomUUID().toString().substring(0, 8);
        for (int i = 0; i < batchSize; i++) {
            Payment p = new Payment(
                    null,
                    Principal.user("USER-" + prefix + "-" + i),
                    "ORDER-PERF-" + prefix + "-" + i,
                    BigDecimal.valueOf(10000),
                    null,
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(10000),
                    PaymentState.PENDING_PG,
                    "PAY-KEY-PERF-" + prefix + "-" + i,
                    PaymentMethod.CARD,
                    null,
                    null,
                    LocalDateTime.now().minusMinutes(10),
                    0
            );
            paymentRepository.save(p);
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch worker1LockAcquiredLatch = new CountDownLatch(1);
        CountDownLatch worker2DoneLatch = new CountDownLatch(1);

        // Worker 1: n개 PENDING_PG 건에 대해 FOR UPDATE SKIP LOCKED 락을 쥐고 대기
        Future<List<Payment>> worker1Future = executor.submit(() -> {
            TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
            List<Payment> lockedPayments = paymentRepository.getPendingPayments(batchSize);
            worker1LockAcquiredLatch.countDown();

            worker2DoneLatch.await(5, TimeUnit.SECONDS);
            transactionManager.rollback(status);
            return lockedPayments;
        });

        worker1LockAcquiredLatch.await(3, TimeUnit.SECONDS);

        // Worker 2: 동시 진입하여 getPendingPayments() 실행 시 락 대기 없이 즉시 Non-blocking 수행
        long startTime = System.currentTimeMillis();
        Future<List<Payment>> worker2Future = executor.submit(() -> {
            TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
            List<Payment> result = paymentRepository.getPendingPayments(batchSize);
            transactionManager.commit(status);
            return result;
        });

        List<Payment> worker2Result = worker2Future.get(3, TimeUnit.SECONDS);
        long elapsedTime = System.currentTimeMillis() - startTime;

        worker2DoneLatch.countDown();
        worker1Future.get();
        executor.shutdown();

        // then
        // 1. Worker 1이 50개 레코드 락을 선점 중이지만, Worker 2는 블로킹 없이 200ms 이내에 즉시 응답 반환 (성능 최적화 검증)
        assertThat(elapsedTime).isLessThan(200);

        // 2. Worker 1이 점유한 레코드는 스킵되어 Worker 2에게는 빈 리스트가 반환됨 (안전한 작업 분할)
        assertThat(worker2Result).isEmpty();
    }
}
