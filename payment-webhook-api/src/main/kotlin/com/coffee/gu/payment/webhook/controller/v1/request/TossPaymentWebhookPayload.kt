package com.coffee.gu.payment.webhook.controller.v1.request

import com.fasterxml.jackson.annotation.JsonProperty

data class TossPaymentCancelPayload(
    @param:JsonProperty("transactionKey") val transactionKey: String? = null,
    @param:JsonProperty("cancelReason") val cancelReason: String? = null,
    @param:JsonProperty("cancelAmount") val cancelAmount: Long? = null,
    @param:JsonProperty("canceledAt") val canceledAt: String? = null,
    @param:JsonProperty("cancelStatus") val cancelStatus: String? = null,
)

data class TossPaymentEasyPayPayload(
    @param:JsonProperty("provider") val provider: String? = null,
)

data class TossPaymentWebhookData(
    @param:JsonProperty("paymentKey") val paymentKey: String? = null,
    @param:JsonProperty("orderId") val orderId: String? = null,
    @param:JsonProperty("status") val status: String? = null,
    @param:JsonProperty("method") val method: String? = null,
    @param:JsonProperty("easyPay") val easyPay: TossPaymentEasyPayPayload? = null,
    @param:JsonProperty("approvedAt") val approvedAt: String? = null,
    @param:JsonProperty("cancels") val cancels: List<TossPaymentCancelPayload>? = null,
    @param:JsonProperty("transactionKey") val transactionKey: String? = null,
    @param:JsonProperty("cancelReason") val cancelReason: String? = null,
    @param:JsonProperty("cancelStatus") val cancelStatus: String? = null,
)

data class TossPaymentWebhookPayload(
    @param:JsonProperty("eventType") val eventType: String? = null,
    @param:JsonProperty("createdAt") val createdAt: String? = null,
    @param:JsonProperty("data") val data: TossPaymentWebhookData? = null,
    @param:JsonProperty("orderId") val orderId: String? = null,
    @param:JsonProperty("status") val status: String? = null,
    @param:JsonProperty("secret") val secret: String? = null,
    @param:JsonProperty("transactionKey") val transactionKey: String? = null,
)
