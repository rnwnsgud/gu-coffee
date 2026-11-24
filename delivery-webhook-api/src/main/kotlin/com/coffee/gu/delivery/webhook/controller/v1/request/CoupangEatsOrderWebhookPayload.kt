package com.coffee.gu.delivery.webhook.controller.v1.request

import com.fasterxml.jackson.annotation.JsonProperty

data class CoupangEatsOrderItemPayload(
    @param:JsonProperty("productCode") val productCode: String = "",
    @param:JsonProperty("qty") val qty: Long = 0L,
)

data class CoupangEatsOrderWebhookPayload(
    @param:JsonProperty("externalOrderId") val externalOrderId: String = "",
    @param:JsonProperty("merchantStoreId") val merchantStoreId: Long = 0L,
    @param:JsonProperty("buyerId") val buyerId: String = "",
    @param:JsonProperty("orderItems") val orderItems: List<CoupangEatsOrderItemPayload> = emptyList(),
)
