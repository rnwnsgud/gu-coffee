package com.coffee.gu.payment.webhook.controller.v1

import com.coffee.gu.payment.webhook.controller.v1.response.TossPaymentWebhookResponse
import com.coffee.gu.payment.webhook.order.TossPaymentWebhookService
import com.coffee.gu.response.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/webhooks/payments")
class TossPaymentWebhookController(
    private val tossPaymentWebhookService: TossPaymentWebhookService,
) {
    companion object {
        const val SIGNATURE_HEADER = "tosspayments-webhook-signature"
        const val TRANSMISSION_TIME_HEADER = "tosspayments-webhook-transmission-time"
    }

    @PostMapping("/toss")
    fun receiveTossPaymentWebhook(
        @RequestHeader(name = SIGNATURE_HEADER, required = false) signature: String?,
        @RequestHeader(name = TRANSMISSION_TIME_HEADER, required = false) transmissionTime: String?,
        @RequestBody rawPayload: String,
        request: HttpServletRequest,
    ): ApiResponse<TossPaymentWebhookResponse> {
        val result = tossPaymentWebhookService.handleWebhook(
            clientIp = request.remoteAddr,
            signature = signature,
            transmissionTime = transmissionTime,
            rawPayload = rawPayload,
        )
        return ApiResponse.success(TossPaymentWebhookResponse.from(result))
    }
}
