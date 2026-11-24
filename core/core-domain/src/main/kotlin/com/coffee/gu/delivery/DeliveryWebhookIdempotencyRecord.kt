package com.coffee.gu.delivery

import com.coffee.gu.enums.DeliveryPlatform

data class DeliveryWebhookIdempotencyRecord(
    val id: Long = 0,
    val platform: DeliveryPlatform,
    val idempotencyKey: String,
    val orderKey: String,
)
