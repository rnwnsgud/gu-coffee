package com.coffee.gu.payment

import com.coffee.gu.enums.OrderState
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.enums.PaymentState
import java.time.LocalDateTime

enum class PaymentWebhookSyncAction {
    PAID,
    CANCELED,
    FAILED,
    IGNORED_ALREADY_PROCESSED,
}

data class PaymentWebhookCommand(
    val orderKey: String,
    val paymentKey: String? = null,
    val status: String,
    val method: PaymentMethod? = null,
    val approveCode: String? = null,
    val approvedAt: LocalDateTime? = null,
    val cancelTransactionKey: String? = null,
    val cancelReason: String? = null,
)

data class PaymentWebhookSyncResult(
    val orderKey: String,
    val paymentState: PaymentState,
    val orderState: OrderState,
    val action: PaymentWebhookSyncAction,
)

interface PaymentWebhookSyncPort {
    fun syncStatus(command: PaymentWebhookCommand): PaymentWebhookSyncResult
}
