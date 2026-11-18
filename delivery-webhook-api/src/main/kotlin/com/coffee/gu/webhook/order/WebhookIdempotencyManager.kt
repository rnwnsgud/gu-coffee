package com.coffee.gu.webhook.order

import com.coffee.gu.enums.DeliveryPlatform
import com.coffee.gu.order.ExternalOrderCreator
import com.coffee.gu.order.NewOrder
import com.coffee.gu.webhook.WebhookIdempotencyRecord
import com.coffee.gu.webhook.WebhookIdempotencyRepository
import org.springframework.stereotype.Component

@Component
class WebhookIdempotencyManager(
    private val webhookIdempotencyRepository: WebhookIdempotencyRepository,
    private val externalOrderCreator: ExternalOrderCreator,
) {
    fun findExistingOrderKey(platform: DeliveryPlatform, idempotencyKey: String): String? {
        return webhookIdempotencyRepository.findByPlatformAndIdempotencyKey(platform, idempotencyKey)?.orderKey
    }

    fun createOrderIdempotently(
        platform: DeliveryPlatform,
        idempotencyKey: String,
        newOrder: NewOrder,
    ): String {
        findExistingOrderKey(platform, idempotencyKey)?.let { return it }
        val orderKey = externalOrderCreator.create(newOrder)
        return try {
            webhookIdempotencyRepository.save(
                WebhookIdempotencyRecord(
                    platform = platform,
                    idempotencyKey = idempotencyKey,
                    orderKey = orderKey,
                ),
            ).orderKey
        } catch (e: RuntimeException) {
            findExistingOrderKey(platform, idempotencyKey) ?: throw e
        }
    }
}
