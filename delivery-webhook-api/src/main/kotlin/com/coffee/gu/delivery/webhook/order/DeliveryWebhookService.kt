package com.coffee.gu.delivery.webhook.order

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.delivery.webhook.security.DeliveryWebhookSecurityValidator
import com.coffee.gu.enums.DeliveryPlatform
import org.springframework.stereotype.Service

@Service
class DeliveryWebhookService(
    private val deliveryWebhookSecurityValidator: DeliveryWebhookSecurityValidator,
    private val deliveryWebhookPayloadProcessor: DeliveryWebhookPayloadProcessor,
    private val deliveryWebhookIdempotencyManager: DeliveryWebhookIdempotencyManager,
) {
    fun receiveBaeminOrder(
        idempotencyKey: String?,
        signature: String?,
        clientIp: String?,
        rawPayload: String,
    ): String {
        val validIdempotencyKey = validateIdempotencyKey(idempotencyKey)
        deliveryWebhookSecurityValidator.validate(DeliveryPlatform.BAEMIN, clientIp, rawPayload, signature)
        deliveryWebhookIdempotencyManager.findExistingOrderKey(DeliveryPlatform.BAEMIN, validIdempotencyKey)?.let { return it }

        val newOrder = deliveryWebhookPayloadProcessor.processBaeminPayload(rawPayload)
        return deliveryWebhookIdempotencyManager.createOrderIdempotently(
            platform = DeliveryPlatform.BAEMIN,
            idempotencyKey = validIdempotencyKey,
            newOrder = newOrder,
        )
    }

    fun receiveCoupangEatsOrder(
        idempotencyKey: String?,
        signature: String?,
        clientIp: String?,
        rawPayload: String,
    ): String {
        val validIdempotencyKey = validateIdempotencyKey(idempotencyKey)
        deliveryWebhookSecurityValidator.validate(DeliveryPlatform.COUPANG_EATS, clientIp, rawPayload, signature)
        deliveryWebhookIdempotencyManager.findExistingOrderKey(DeliveryPlatform.COUPANG_EATS, validIdempotencyKey)?.let { return it }

        val newOrder = deliveryWebhookPayloadProcessor.processCoupangEatsPayload(rawPayload)
        return deliveryWebhookIdempotencyManager.createOrderIdempotently(
            platform = DeliveryPlatform.COUPANG_EATS,
            idempotencyKey = validIdempotencyKey,
            newOrder = newOrder,
        )
    }

    private fun validateIdempotencyKey(idempotencyKey: String?): String {
        val normalized = idempotencyKey?.trim()
        if (normalized.isNullOrBlank()) {
            throw CoreException(ErrorType.INVALID_REQUEST, "웹훅 멱등성 키 헤더가 누락되었습니다.")
        }
        return normalized
    }
}
