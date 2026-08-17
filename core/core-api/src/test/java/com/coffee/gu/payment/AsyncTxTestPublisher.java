package com.coffee.gu.payment;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.PaymentMethod;
import com.coffee.gu.enums.PaymentState;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Component
public class AsyncTxTestPublisher {

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    public AsyncTxTestPublisher(PaymentRepository paymentRepository, ApplicationEventPublisher eventPublisher, EntityManager entityManager) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
    }

    @Transactional
    public CompletableFuture<Boolean> publishStandardAsyncEventInTransaction(String orderKey) {
        Payment payment = new Payment(
                null, Principal.user("U100"), orderKey, BigDecimal.TEN, null, BigDecimal.ZERO, BigDecimal.TEN,
                PaymentState.PENDING_PG, "PAY-KEY-" + orderKey, PaymentMethod.CARD, null, null, LocalDateTime.now(), 0
        );
        paymentRepository.save(payment);
        entityManager.flush(); // DB SQL 발행 (트랜잭션 미커밋 상태)

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        eventPublisher.publishEvent(new TestPaymentCreatedEvent(orderKey, future));

        try {
            // 비동기 스레드가 미커밋 상태일 때 DB 조회를 시도하도록 메인 트랜잭션을 500ms 동안 유지
            Thread.sleep(500);
        } catch (InterruptedException ignored) {}

        return future;
    }

    @Transactional
    public CompletableFuture<Boolean> publishTransactionalAsyncEventInTransaction(String orderKey) {
        Payment payment = new Payment(
                null, Principal.user("U100"), orderKey, BigDecimal.TEN, null, BigDecimal.ZERO, BigDecimal.TEN,
                PaymentState.PENDING_PG, "PAY-KEY-" + orderKey, PaymentMethod.CARD, null, null, LocalDateTime.now(), 0
        );
        paymentRepository.save(payment);

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        eventPublisher.publishEvent(new TestTxPaymentCreatedEvent(orderKey, future));

        return future;
    }
}
