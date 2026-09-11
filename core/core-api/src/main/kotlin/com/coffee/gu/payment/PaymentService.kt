package com.coffee.gu.payment

import com.coffee.gu.CancelEvent
import com.coffee.gu.PGConfirmResult
import com.coffee.gu.PGPayment
import com.coffee.gu.PaymentGatewayCancel
import com.coffee.gu.PaymentGatewayConfirm
import com.coffee.gu.PaymentGatewayStatus
import com.coffee.gu.enums.PaymentMethod
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
        val pgConfirmResult: PGConfirmResult = try {
            paymentGatewayProcessor.approvePayment(
                PaymentGatewayConfirm(pgPayment.paymentKey, pgPayment.orderKey, pgPayment.amount)
            )
        } catch (e: Exception) {
            return resolveApprovalFailure(order, payment, pgPayment)
        }
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

    private fun resolveApprovalFailure(
        order: Order,
        payment: Payment,
        pgPayment: PGPayment,
    ): PaymentApprovalResult {
        return try {
            val currentPg = paymentGatewayProcessor.getPGPayment(order.key)
            if (currentPg.status == PaymentGatewayStatus.DONE) {
                val recoveredResult = PGConfirmResult.success(
                    currentPg.orderKey,
                    currentPg.paymentKey,
                    payment.method,
                    payment.approveCode,
                    payment.paidAt
                )
                paymentCompleter.complete(order, payment.id, recoveredResult)
            } else {
                paymentCompleter.failProcess(order, payment, "PG_APPROVE_TIMEOUT", "승인 타임아웃 후 미승인 확인")
                PaymentApprovalResult.failed(order.key, payment.externalPaymentKey ?: "", OffsetDateTime.now())
            }
        } catch (inquiryEx: Exception) {
            try {
                paymentGatewayProcessor.cancelPayment(
                    PaymentGatewayCancel(pgPayment.paymentKey, "승인 응답 및 상태 조회 타임아웃에 따른 자동 망취소")
                )
            } catch (cancelEx: Exception) {
                val event = CancelEvent(order.key)
                outboxEventPublisher.publishOutboxEvent(event)
            }
            paymentCompleter.failProcess(order, payment, "NETWORK_TIMEOUT", "승인 및 상태 조회 네트워크 타임아웃")
            PaymentApprovalResult.failed(order.key, payment.externalPaymentKey ?: "", OffsetDateTime.now())
        }
    }

}
