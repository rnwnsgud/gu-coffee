package com.coffee.gu.payment

import com.coffee.gu.Principal
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.enums.PaymentState
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

@Component
class AsyncTxTestPublisher(
    private val paymentRepository: PaymentRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val entityManager: EntityManager
) {
    @Transactional
    fun publishStandardAsyncEventInTransaction(orderKey: String): CompletableFuture<Boolean> {
        val payment = Payment(
            id = 0L,
            principal = Principal.user("U100"),
            orderKey = orderKey,
            originalAmount = BigDecimal.TEN,
            issuedCouponId = null,
            couponDiscount = null,
            amount = BigDecimal.TEN,
            state = PaymentState.PENDING_PG,
            externalPaymentKey = "PAY-KEY-$orderKey",
            method = PaymentMethod.CARD,
            paidAt = null,
            approveCode = null,
            createdAt = LocalDateTime.now(),
            retryCount = 0
        )
        paymentRepository.save(payment)
        entityManager.flush() // DB SQL 발행 (트랜잭션 미커밋 상태)

        val future = CompletableFuture<Boolean>()
        eventPublisher.publishEvent(TestPaymentCreatedEvent(orderKey, future))

        try {
            // 비동기 스레드가 미커밋 상태일 때 DB 조회를 시도하도록 메인 트랜잭션을 500ms 동안 유지
            Thread.sleep(500)
        } catch (ignored: InterruptedException) {}

        return future
    }

    @Transactional
    fun publishTransactionalAsyncEventInTransaction(orderKey: String): CompletableFuture<Boolean> {
        val payment = Payment(
            id = 0L,
            principal = Principal.user("U100"),
            orderKey = orderKey,
            originalAmount = BigDecimal.TEN,
            issuedCouponId = null,
            couponDiscount = null,
            amount = BigDecimal.TEN,
            state = PaymentState.PENDING_PG,
            externalPaymentKey = "PAY-KEY-$orderKey",
            method = PaymentMethod.CARD,
            paidAt = null,
            approveCode = null,
            createdAt = LocalDateTime.now(),
            retryCount = 0
        )
        paymentRepository.save(payment)

        val future = CompletableFuture<Boolean>()
        eventPublisher.publishEvent(TestTxPaymentCreatedEvent(orderKey, future))

        return future
    }
}
