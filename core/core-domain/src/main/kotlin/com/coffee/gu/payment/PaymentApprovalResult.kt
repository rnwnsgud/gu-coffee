package com.coffee.gu.payment

import com.coffee.gu.enums.OrderState
import com.coffee.gu.enums.PaymentState
import java.time.OffsetDateTime

class PaymentApprovalResult(
    val orderKey: String,
    val paymentKey: String,
    val paymentState: PaymentState,
    val orderState: OrderState,
    val idempotent: Boolean,
    val approvedAt: OffsetDateTime?,
) {
    companion object {
        fun approved(
            orderKey: String,
            paymentKey: String,
            approvedAt: OffsetDateTime,
        ): PaymentApprovalResult = PaymentApprovalResult(
            orderKey = orderKey,
            paymentKey = paymentKey,
            paymentState = PaymentState.SUCCESS,
            orderState = OrderState.PAID,
            idempotent = false,
            approvedAt = approvedAt,
        )

        fun alreadyApproved(
            orderKey: String,
            paymentKey: String,
            approvedAt: OffsetDateTime,
        ): PaymentApprovalResult = PaymentApprovalResult(
            orderKey = orderKey,
            paymentKey = paymentKey,
            paymentState = PaymentState.SUCCESS,
            orderState = OrderState.PAID,
            idempotent = true,
            approvedAt = approvedAt,
        )

        fun fromExisting(payment: Payment): PaymentApprovalResult {
            val orderState = if (payment.state == PaymentState.SUCCESS) OrderState.PAID else OrderState.CREATED
            return PaymentApprovalResult(
                orderKey = payment.orderKey,
                paymentKey = payment.externalPaymentKey ?: "",
                paymentState = payment.state,
                orderState = orderState,
                idempotent = true,
                approvedAt = payment.paidAt,
            )
        }

        fun failed(
            orderKey: String,
            paymentKey: String,
            approvedAt: OffsetDateTime,
        ): PaymentApprovalResult = PaymentApprovalResult(
            orderKey = orderKey,
            paymentKey = paymentKey,
            paymentState = PaymentState.FAILED,
            orderState = OrderState.PAID,
            idempotent = false,
            approvedAt = approvedAt,
        )
    }
}
