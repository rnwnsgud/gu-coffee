package com.coffee.gu.payment;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Component
public class AsyncTxTestListener {

    private final PaymentRepository paymentRepository;

    public AsyncTxTestListener(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    // 🔴 문제 상황: 일반 @EventListener + @Async (메인 트랜잭션 미커밋 시점 실행)
    @Async
    @EventListener
    public void handleStandardAsync(TestPaymentCreatedEvent event) {
        Optional<Payment> found = paymentRepository.findByOrderKey(event.orderKey());
        event.future().complete(found.isPresent());
    }

    // 🟢 해결 방법: @TransactionalEventListener(phase = AFTER_COMMIT) + @Async (메인 트랜잭션 커밋 완료 후 실행)
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransactionalAsync(TestTxPaymentCreatedEvent event) {
        Optional<Payment> found = paymentRepository.findByOrderKey(event.orderKey());
        event.future().complete(found.isPresent());
    }
}
