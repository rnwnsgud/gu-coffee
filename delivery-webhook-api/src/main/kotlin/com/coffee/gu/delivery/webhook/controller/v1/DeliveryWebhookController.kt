package com.coffee.gu.delivery.webhook.controller.v1

import com.coffee.gu.delivery.webhook.controller.v1.response.DeliveryWebhookOrderResponse
import com.coffee.gu.delivery.webhook.order.DeliveryWebhookService
import com.coffee.gu.enums.DeliveryPlatform
import com.coffee.gu.response.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/webhooks/orders")
class DeliveryWebhookController(
    private val deliveryWebhookService: DeliveryWebhookService,
) {
    companion object {
        const val SIGNATURE_HEADER = "X-Webhook-Signature"
        const val IDEMPOTENCY_KEY_HEADER = "X-Webhook-Idempotency-Key"
    }

    @PostMapping("/baemin")
    fun receiveBaeminOrder(
        @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) idempotencyKey: String?,
        @RequestHeader(name = SIGNATURE_HEADER, required = false) signature: String?,
        @RequestBody rawPayload: String,
        request: HttpServletRequest,
    ): ApiResponse<DeliveryWebhookOrderResponse> {
        val orderKey = deliveryWebhookService.receiveBaeminOrder(
            idempotencyKey = idempotencyKey,
            signature = signature,
            clientIp = request.remoteAddr,
            rawPayload = rawPayload,
        )
        return ApiResponse.success(
            DeliveryWebhookOrderResponse(
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
    ): ApiResponse<DeliveryWebhookOrderResponse> {
        val orderKey = deliveryWebhookService.receiveCoupangEatsOrder(
            idempotencyKey = idempotencyKey,
            signature = signature,
            clientIp = request.remoteAddr,
            rawPayload = rawPayload,
        )
        return ApiResponse.success(
            DeliveryWebhookOrderResponse(
                orderKey = orderKey,
                platform = DeliveryPlatform.COUPANG_EATS,
                idempotencyKey = idempotencyKey!!.trim(),
            ),
        )
    }
}
