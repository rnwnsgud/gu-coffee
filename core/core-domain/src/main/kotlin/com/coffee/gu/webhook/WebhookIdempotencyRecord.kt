package com.coffee.gu.webhook

import com.coffee.gu.enums.DeliveryPlatform

data class WebhookIdempotencyRecord(
    val id: Long = 0,
    val platform: DeliveryPlatform,
    val idempotencyKey: String,
    val orderKey: String,
)
