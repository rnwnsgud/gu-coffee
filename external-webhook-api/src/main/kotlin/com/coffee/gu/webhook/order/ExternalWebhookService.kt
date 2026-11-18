package com.coffee.gu.webhook.order

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.enums.DeliveryPlatform
import com.coffee.gu.webhook.security.WebhookSecurityValidator
import org.springframework.stereotype.Service

@Service
class ExternalWebhookService(
    private val webhookSecurityValidator: WebhookSecurityValidator,
    private val webhookPayloadProcessor: WebhookPayloadProcessor,
    private val webhookIdempotencyManager: WebhookIdempotencyManager,
) {
    fun receiveBaeminOrder(
        idempotencyKey: String?,
        signature: String?,
        clientIp: String?,
        rawPayload: String,
    ): String {
        val validIdempotencyKey = validateIdempotencyKey(idempotencyKey)
        webhookSecurityValidator.validate(DeliveryPlatform.BAEMIN, clientIp, rawPayload, signature)
        webhookIdempotencyManager.findExistingOrderKey(DeliveryPlatform.BAEMIN, validIdempotencyKey)?.let { return it }

        val newOrder = webhookPayloadProcessor.processBaeminPayload(rawPayload)
        return webhookIdempotencyManager.createOrderIdempotently(
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
        webhookSecurityValidator.validate(DeliveryPlatform.COUPANG_EATS, clientIp, rawPayload, signature)
        webhookIdempotencyManager.findExistingOrderKey(DeliveryPlatform.COUPANG_EATS, validIdempotencyKey)?.let { return it }

        val newOrder = webhookPayloadProcessor.processCoupangEatsPayload(rawPayload)
        return webhookIdempotencyManager.createOrderIdempotently(
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
