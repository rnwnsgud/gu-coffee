package com.coffee.gu.payment

import com.coffee.gu.CancelEvent
import com.coffee.gu.PGConfirmResult
import com.coffee.gu.PGPayment
import com.coffee.gu.PaymentGatewayConfirm
import com.coffee.gu.event.OutboxEventPublisher
import com.coffee.gu.order.Order
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class PaymentService(
    private val paymentGatewayProcessor: PaymentGatewayProcessor,
    private val paymentPreparer: PaymentPreparer,
    private val paymentCompleter: PaymentCompleter,
    private val paymentManager: PaymentManager,
    private val paymentReader: PaymentReader,
    private val outboxEventPublisher: OutboxEventPublisher,
) {
    fun createPayment(order: Order, paymentDiscount: PaymentDiscount): Long {
        return paymentManager.createPayment(order, paymentDiscount)
    }

    fun approvePayment(order: Order): PaymentApprovalResult {
        var payment = paymentReader.getByOrderKey(order.key)
        if (!payment.isReady) return PaymentApprovalResult.fromExisting(payment)
        val pgPayment = paymentGatewayProcessor.getPGPayment(order.key)
        payment = paymentPreparer.prepare(order, pgPayment)
        val pgConfirmResult = paymentGatewayProcessor.approvePayment(
            PaymentGatewayConfirm(pgPayment.paymentKey, pgPayment.orderKey, pgPayment.amount)
        )
        return try {
            paymentCompleter.complete(order, payment.id, pgConfirmResult)
        } catch (e: Exception) {
            val event = CancelEvent(order.key)
            outboxEventPublisher.publishOutboxEvent(event)
            PaymentApprovalResult.failed(
                order.key,
                payment.externalPaymentKey ?: "",
                payment.paidAt ?: OffsetDateTime.now()
            )
        }
    }

    fun fail(order: Order, code: String, message: String) {
        val payment = paymentReader.getByOrderKey(order.key)
        paymentCompleter.failProcess(order, payment, code, message)
    }
}
