package com.coffee.gu.delivery.webhook.controller.v1.response

import com.coffee.gu.enums.DeliveryPlatform

data class DeliveryWebhookOrderResponse(
    val orderKey: String,
    val platform: DeliveryPlatform,
    val idempotencyKey: String,
)
