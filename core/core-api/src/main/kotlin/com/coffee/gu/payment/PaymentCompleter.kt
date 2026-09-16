package com.coffee.gu.payment

import com.coffee.gu.CancelEvent
import com.coffee.gu.PGConfirmResult
import com.coffee.gu.PaymentApprovedEvent
import com.coffee.gu.TransactionHistoryManager
import com.coffee.gu.coupon.IssuedCouponManager
import com.coffee.gu.enums.TransactionType
import com.coffee.gu.event.OutboxEventPublisher
import com.coffee.gu.order.Order
import com.coffee.gu.order.OrderManager
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Component
class PaymentCompleter(
    private val paymentReader: PaymentReader,
    private val paymentManager: PaymentManager,
    private val orderManager: OrderManager,
    private val issuedCouponManager: IssuedCouponManager,
    private val transactionHistoryManager: TransactionHistoryManager,
    private val outboxEventPublisher: OutboxEventPublisher,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun complete(order: Order, paymentId: Long, confirmedPayment: PGConfirmResult): PaymentApprovalResult {
        val payment = paymentReader.getByIdWithLock(paymentId)
        val extKey = payment.externalPaymentKey ?: ""
        val paidAt = payment.paidAt ?: OffsetDateTime.now()

        if (payment.isPaid) return PaymentApprovalResult.alreadyApproved(order.key, extKey, paidAt)
        if (payment.isFailed) return PaymentApprovalResult.failed(order.key, extKey, paidAt)
        if (!confirmedPayment.isConfirmed) {
            payment.fail()
            paymentManager.save(payment)
            return PaymentApprovalResult.failed(order.key, extKey, paidAt)
        }
        paymentManager.pay(payment, confirmedPayment)
        orderManager.pay(order)
        issuedCouponManager.use(payment)
        transactionHistoryManager.record(TransactionType.PAYMENT, order, payment, "Payment processed", payment.paidAt)
        applicationEventPublisher.publishEvent(PaymentApprovedEvent(order.key, payment.hasAppliedCoupon()))
        return PaymentApprovalResult.approved(order.key, extKey, paidAt)
    }

    @Transactional
    fun compensateApprovalFailure(
        order: Order,
        paymentId: Long,
        externalPaymentKey: String,
        reason: String,
    ): PaymentApprovalResult {
        val payment = paymentReader.getByIdWithLock(paymentId)
        payment.fail(externalPaymentKey)
        paymentManager.save(payment)
        transactionHistoryManager.record(
            TransactionType.PAYMENT_FAIL,
            order,
            payment,
            reason,
            OffsetDateTime.now()
        )
        val event = CancelEvent(order.key)
        outboxEventPublisher.publishOutboxEvent(event)
        return PaymentApprovalResult.failed(order.key, externalPaymentKey, OffsetDateTime.now())
    }

    @Transactional
    fun failProcess(order: Order, payment: Payment, code: String, message: String) {
        payment.fail()
        paymentManager.save(payment)
        transactionHistoryManager.record(TransactionType.PAYMENT_FAIL, order, payment, "$code $message", OffsetDateTime.now())
    }
}
