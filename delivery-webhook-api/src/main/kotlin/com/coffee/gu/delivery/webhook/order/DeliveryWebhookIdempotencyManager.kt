package com.coffee.gu.delivery.webhook.order

import com.coffee.gu.delivery.DeliveryWebhookIdempotencyRecord
import com.coffee.gu.delivery.DeliveryWebhookIdempotencyRepository
import com.coffee.gu.enums.DeliveryPlatform
import com.coffee.gu.order.ExternalOrderCreator
import com.coffee.gu.order.NewOrder
import org.springframework.stereotype.Component

@Component
class DeliveryWebhookIdempotencyManager(
    private val deliveryWebhookIdempotencyRepository: DeliveryWebhookIdempotencyRepository,
    private val externalOrderCreator: ExternalOrderCreator,
) {
    fun findExistingOrderKey(platform: DeliveryPlatform, idempotencyKey: String): String? {
        return deliveryWebhookIdempotencyRepository.findByPlatformAndIdempotencyKey(platform, idempotencyKey)?.orderKey
    }

    fun createOrderIdempotently(
        platform: DeliveryPlatform,
        idempotencyKey: String,
        newOrder: NewOrder,
    ): String {
        findExistingOrderKey(platform, idempotencyKey)?.let { return it }
        val orderKey = externalOrderCreator.create(newOrder)
        return try {
            deliveryWebhookIdempotencyRepository.save(
                DeliveryWebhookIdempotencyRecord(
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
