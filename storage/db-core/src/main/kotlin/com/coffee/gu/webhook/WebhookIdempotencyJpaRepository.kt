package com.coffee.gu.webhook

import com.coffee.gu.enums.DeliveryPlatform
import org.springframework.data.jpa.repository.JpaRepository

interface WebhookIdempotencyJpaRepository : JpaRepository<WebhookIdempotencyEntity, Long> {
    fun findByPlatformAndIdempotencyKey(
        platform: DeliveryPlatform,
        idempotencyKey: String,
    ): WebhookIdempotencyEntity?
}
