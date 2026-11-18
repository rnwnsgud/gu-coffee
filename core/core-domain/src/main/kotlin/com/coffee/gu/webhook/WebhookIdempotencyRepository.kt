package com.coffee.gu.webhook

import com.coffee.gu.enums.DeliveryPlatform

interface WebhookIdempotencyRepository {
    fun findByPlatformAndIdempotencyKey(
        platform: DeliveryPlatform,
        idempotencyKey: String,
    ): WebhookIdempotencyRecord?

    fun save(record: WebhookIdempotencyRecord): WebhookIdempotencyRecord
}
