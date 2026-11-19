package com.coffee.gu.delivery

import com.coffee.gu.enums.DeliveryPlatform

interface DeliveryWebhookIdempotencyRepository {
    fun findByPlatformAndIdempotencyKey(
        platform: DeliveryPlatform,
        idempotencyKey: String,
    ): DeliveryWebhookIdempotencyRecord?

    fun save(record: DeliveryWebhookIdempotencyRecord): DeliveryWebhookIdempotencyRecord
}
