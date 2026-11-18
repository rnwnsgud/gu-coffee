package com.coffee.gu.webhook.controller.v1.response

import com.coffee.gu.enums.DeliveryPlatform

data class ExternalWebhookOrderResponse(
    val orderKey: String,
    val platform: DeliveryPlatform,
    val idempotencyKey: String,
)
