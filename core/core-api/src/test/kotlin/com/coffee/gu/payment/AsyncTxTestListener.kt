package com.coffee.gu.payment

import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AsyncTxTestListener(
    private val paymentRepository: PaymentRepository
) {
    @Async
    @EventListener
    fun handleStandardAsync(event: TestPaymentCreatedEvent) {
        try {
            paymentRepository.findByOrderKey(event.orderKey)
            event.future.complete(true)
        } catch (e: Exception) {
            event.future.complete(false)
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleTransactionalAsync(event: TestTxPaymentCreatedEvent) {
        try {
            paymentRepository.findByOrderKey(event.orderKey)
            event.future.complete(true)
        } catch (e: Exception) {
            event.future.complete(false)
        }
    }
}
