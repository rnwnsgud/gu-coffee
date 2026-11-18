package com.coffee.gu.webhook.controller.v1

import com.coffee.gu.enums.DeliveryPlatform
import com.coffee.gu.response.ApiResponse
import com.coffee.gu.webhook.controller.v1.response.ExternalWebhookOrderResponse
import com.coffee.gu.webhook.order.ExternalWebhookService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/webhooks/orders")
class ExternalWebhookController(
    private val externalWebhookService: ExternalWebhookService,
) {
    companion object {
        const val SIGNATURE_HEADER = "X-Webhook-Signature"
        const val IDEMPOTENCY_KEY_HEADER = "X-Webhook-Idempotency-Key"
        const val FORWARDED_FOR_HEADER = "X-Forwarded-For"
    }

    @PostMapping("/baemin")
    fun receiveBaeminOrder(
        @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) idempotencyKey: String?,
        @RequestHeader(name = SIGNATURE_HEADER, required = false) signature: String?,
        @RequestBody rawPayload: String,
        request: HttpServletRequest,
    ): ApiResponse<ExternalWebhookOrderResponse> {
        val orderKey = externalWebhookService.receiveBaeminOrder(
            idempotencyKey = idempotencyKey,
            signature = signature,
            clientIp = resolveClientIp(request),
            rawPayload = rawPayload,
        )
        return ApiResponse.success(
            ExternalWebhookOrderResponse(
                orderKey = orderKey,
                platform = DeliveryPlatform.BAEMIN,
                idempotencyKey = idempotencyKey!!.trim(),
            ),
        )
    }

    @PostMapping("/coupang-eats")
    fun receiveCoupangEatsOrder(
        @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) idempotencyKey: String?,
        @RequestHeader(name = SIGNATURE_HEADER, required = false) signature: String?,
        @RequestBody rawPayload: String,
        request: HttpServletRequest,
    ): ApiResponse<ExternalWebhookOrderResponse> {
        val orderKey = externalWebhookService.receiveCoupangEatsOrder(
            idempotencyKey = idempotencyKey,
            signature = signature,
            clientIp = resolveClientIp(request),
            rawPayload = rawPayload,
        )
        return ApiResponse.success(
            ExternalWebhookOrderResponse(
                orderKey = orderKey,
                platform = DeliveryPlatform.COUPANG_EATS,
                idempotencyKey = idempotencyKey!!.trim(),
            ),
        )
    }

    private fun resolveClientIp(request: HttpServletRequest): String? {
        val forwardedFor = request.getHeader(FORWARDED_FOR_HEADER)
        if (!forwardedFor.isNullOrBlank()) {
            return forwardedFor.substringBefore(",").trim()
        }
        return request.remoteAddr
    }
}
