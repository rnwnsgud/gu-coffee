package com.coffee.gu.delivery

import com.coffee.gu.enums.DeliveryPlatform
import org.springframework.data.jpa.repository.JpaRepository

interface DeliveryWebhookIdempotencyJpaRepository : JpaRepository<DeliveryWebhookIdempotencyEntity, Long> {
    fun findByPlatformAndIdempotencyKey(
        platform: DeliveryPlatform,
        idempotencyKey: String,
    ): DeliveryWebhookIdempotencyEntity?
}
