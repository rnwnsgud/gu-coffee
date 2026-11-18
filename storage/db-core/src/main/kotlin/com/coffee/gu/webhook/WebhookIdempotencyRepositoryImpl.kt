package com.coffee.gu.webhook

import com.coffee.gu.enums.DeliveryPlatform
import org.springframework.stereotype.Repository

@Repository
class WebhookIdempotencyRepositoryImpl(
    private val webhookIdempotencyJpaRepository: WebhookIdempotencyJpaRepository,
) : WebhookIdempotencyRepository {

    override fun findByPlatformAndIdempotencyKey(
        platform: DeliveryPlatform,
        idempotencyKey: String,
    ): WebhookIdempotencyRecord? {
        return webhookIdempotencyJpaRepository.findByPlatformAndIdempotencyKey(platform, idempotencyKey)?.toModel()
    }

    override fun save(record: WebhookIdempotencyRecord): WebhookIdempotencyRecord {
        val savedRecord = webhookIdempotencyJpaRepository.save(WebhookIdempotencyEntity.from(record))
        return savedRecord.toModel()
    }
}
