package com.coffee.gu.payment;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.PaymentMethod;
import com.coffee.gu.enums.PaymentState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PaymentRepositorySkipLockTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("getPendingPayments 호출 시 다른 트랜잭션이 락을 쥔 레코드는 대기 없이 스킵(SKIP LOCKED)한다")
    void testGetPendingPaymentsSkipLock() throws Exception {
        // given
        Payment p1 = new Payment(null, Principal.user("U1"), "ORDER-SKIP-1", BigDecimal.valueOf(10000), null, BigDecimal.ZERO, BigDecimal.valueOf(10000), PaymentState.PENDING_PG, "PAY-KEY-1", PaymentMethod.CARD, null, null, LocalDateTime.now().minusMinutes(10), 0);
        Payment p2 = new Payment(null, Principal.user("U2"), "ORDER-SKIP-2", BigDecimal.valueOf(20000), null, BigDecimal.ZERO, BigDecimal.valueOf(20000), PaymentState.PENDING_PG, "PAY-KEY-2", PaymentMethod.CARD, null, null, LocalDateTime.now().minusMinutes(10), 0);

        paymentRepository.save(p1);
        paymentRepository.save(p2);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch tx1LockedLatch = new CountDownLatch(1);
        CountDownLatch tx2FinishLatch = new CountDownLatch(1);

        // Tx1: getPendingPayments()를 통해 P1, P2 레코드에 FOR UPDATE SKIP LOCKED 락을 잡음
        Future<List<Payment>> tx1Future = executor.submit(() -> {
            TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
            List<Payment> lockedPayments = paymentRepository.getPendingPayments(2);
            tx1LockedLatch.countDown();

            tx2FinishLatch.await(5, TimeUnit.SECONDS);
            transactionManager.rollback(status);
            return lockedPayments;
        });

        tx1LockedLatch.await(3, TimeUnit.SECONDS);

        // Tx2: 다른 트랜잭션에서 getPendingPayments() 실행 ➔ 대기 없이 Tx1이 잡은 레코드를 스킵
        long startTime = System.currentTimeMillis();
        Future<List<Payment>> tx2Future = executor.submit(() -> {
            TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
            List<Payment> result = paymentRepository.getPendingPayments(2);
            transactionManager.commit(status);
            return result;
        });

        List<Payment> tx2Result = tx2Future.get(3, TimeUnit.SECONDS);
        long elapsedTime = System.currentTimeMillis() - startTime;

        tx2FinishLatch.countDown();
        tx1Future.get();
        executor.shutdown();

        // then
        assertThat(elapsedTime).isLessThan(2000);
        assertThat(tx2Result).isEmpty();
    }
}
