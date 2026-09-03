package com.coffee.gu.toss

import com.coffee.gu.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body
import java.nio.charset.StandardCharsets
import java.util.Base64

@Component
class TossPaymentsGateway(
    private val restClient: RestClient,
    private val properties: TossPaymentsProperties,
    private val methodMapper: TossPaymentsMethodMapper
) : PaymentGateway {

    companion object {
        private val log = LoggerFactory.getLogger(TossPaymentsGateway::class.java)
    }

    override fun provider(): PaymentGatewayProvider {
        return PaymentGatewayProvider.TOSS_PAYMENTS
    }

    override fun confirm(confirm: PaymentGatewayConfirm): PGConfirmResult {
        val request = TossPaymentsConfirmRequest(confirm.paymentKey, confirm.orderKey, confirm.amount)
        try {
            val response = restClient.post()
                .uri("${properties.baseUrl}/payments/confirm")
                .header("Authorization", basicAuthorizationHeader(properties.secretKey))
                .header("Idempotency-Key", confirm.paymentKey)
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve()
                .body(Payment::class.java)
                ?: throw CoreException(ErrorType.PAYMENT_FAIL, "토스페이먼츠 승인 응답이 비어 있습니다.")

            return PGConfirmResult.success(
                orderId = response.orderId,
                paymentKey = response.paymentKey,
                paymentMethod = methodMapper.map(response),
                approveCode = response.card?.approveNo,
                approvedAt = response.approvedAt
            )
        } catch (e: RestClientResponseException) {
            if (e.statusCode.is4xxClientError) {
                return PGConfirmResult.fail(request.orderId, request.paymentKey)
            }
            throw e
        }
    }

    override fun getByOrderKey(orderKey: String): PGPayment {
        val response = restClient.get()
            .uri("${properties.baseUrl}/payments/orders/$orderKey")
            .header("Authorization", basicAuthorizationHeader(properties.secretKey))
            .retrieve()
            .body<Payment>()
            ?: throw CoreException(ErrorType.PAYMENT_FAIL, "토스페이먼츠 승인 응답이 비어 있습니다.")

        return PGPayment(
            paymentKey = response.paymentKey,
            orderKey = response.orderId,
            amount = response.totalAmount,
            status = PaymentGatewayStatus.valueOf(response.status.name)
        )
    }

    override fun cancel(cancel: PaymentGatewayCancel): PGCancelResult {
        val request = TossPaymentsCancelRequest(cancel.cancelReason)
        try {
            val response = restClient.post()
                .uri("${properties.baseUrl}/payments/${cancel.paymentKey}/cancel")
                .header("Authorization", basicAuthorizationHeader(properties.secretKey))
                .header("Idempotency-Key", cancel.paymentKey)
                .body(request)
                .retrieve()
                .body(Payment::class.java)
                ?: throw CoreException(ErrorType.PAYMENT_FAIL, "토스페이먼츠 승인 응답이 비어 있습니다.")

            return PGCancelResult()
        } catch (e: RestClientResponseException) {
            if (e.statusCode.is4xxClientError) {
            }
            throw e
        }
    }

    private fun basicAuthorizationHeader(secretKey: String): String {
        val credential = "$secretKey:"
        val encodedCredential = Base64.getEncoder()
            .encodeToString(credential.toByteArray(StandardCharsets.UTF_8))
        return "Basic $encodedCredential"
    }
}
