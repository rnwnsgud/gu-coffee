package com.coffee.gu.delivery

import com.coffee.gu.enums.DeliveryPlatform
import org.springframework.stereotype.Repository

@Repository
class DeliveryWebhookIdempotencyRepositoryImpl(
    private val deliveryWebhookIdempotencyJpaRepository: DeliveryWebhookIdempotencyJpaRepository,
) : DeliveryWebhookIdempotencyRepository {

    override fun findByPlatformAndIdempotencyKey(
        platform: DeliveryPlatform,
        idempotencyKey: String,
    ): DeliveryWebhookIdempotencyRecord? {
        return deliveryWebhookIdempotencyJpaRepository
            .findByPlatformAndIdempotencyKey(platform, idempotencyKey)
            ?.toModel()
    }

    override fun save(record: DeliveryWebhookIdempotencyRecord): DeliveryWebhookIdempotencyRecord {
        val savedRecord = deliveryWebhookIdempotencyJpaRepository.save(
            DeliveryWebhookIdempotencyEntity.from(record),
        )
        return savedRecord.toModel()
    }
}
