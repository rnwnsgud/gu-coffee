package com.coffee.gu.payment.webhook.order

import com.coffee.gu.payment.PaymentWebhookSyncPort
import com.coffee.gu.payment.PaymentWebhookSyncResult
import com.coffee.gu.payment.webhook.security.TossWebhookSecurityValidator
import org.springframework.stereotype.Service

@Service
class TossPaymentWebhookService(
    private val tossWebhookSecurityValidator: TossWebhookSecurityValidator,
    private val tossWebhookPayloadProcessor: TossWebhookPayloadProcessor,
    private val paymentWebhookSyncPort: PaymentWebhookSyncPort,
) {
    fun handleWebhook(
        clientIp: String?,
        signature: String?,
        transmissionTime: String?,
        rawPayload: String,
    ): PaymentWebhookSyncResult {
        tossWebhookSecurityValidator.validate(
            clientIp = clientIp,
            rawPayload = rawPayload,
            signature = signature,
            transmissionTime = transmissionTime,
        )
        val command = tossWebhookPayloadProcessor.process(rawPayload)
        return paymentWebhookSyncPort.syncStatus(command)
    }
}
