package com.coffee.gu.payment.webhook.controller.v1.response

import com.coffee.gu.enums.OrderState
import com.coffee.gu.enums.PaymentState
import com.coffee.gu.payment.PaymentWebhookSyncAction
import com.coffee.gu.payment.PaymentWebhookSyncResult

data class TossPaymentWebhookResponse(
    val orderKey: String,
    val paymentState: PaymentState,
    val orderState: OrderState,
    val action: PaymentWebhookSyncAction,
) {
    companion object {
        fun from(result: PaymentWebhookSyncResult): TossPaymentWebhookResponse {
            return TossPaymentWebhookResponse(
                orderKey = result.orderKey,
                paymentState = result.paymentState,
                orderState = result.orderState,
                action = result.action,
            )
        }
    }
}
