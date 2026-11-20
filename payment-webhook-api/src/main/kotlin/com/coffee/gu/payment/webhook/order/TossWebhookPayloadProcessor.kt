package com.coffee.gu.payment.webhook.order

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.payment.PaymentWebhookCommand
import com.coffee.gu.payment.webhook.controller.v1.request.TossPaymentWebhookPayload
import com.coffee.gu.toss.TossPaymentsMethodMapper
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.time.OffsetDateTime

@Component
class TossWebhookPayloadProcessor(
    private val jsonMapper: JsonMapper,
    private val tossPaymentsMethodMapper: TossPaymentsMethodMapper,
) {
    fun process(rawPayload: String): PaymentWebhookCommand {
        val payload = try {
            jsonMapper.readValue(rawPayload, TossPaymentWebhookPayload::class.java)
        } catch (_: Exception) {
            throw CoreException(ErrorType.INVALID_REQUEST, "토스페이먼츠 웹훅 JSON 형식이 올바르지 않습니다.")
        }

        val eventType = payload.eventType?.trim()?.uppercase()
        if (eventType.isNullOrBlank()) {
            return processDepositCallback(payload)
        }

        return when (eventType) {
            "PAYMENT_STATUS_CHANGED" -> processPaymentStatusChanged(payload)
            "CANCEL_STATUS_CHANGED" -> processCancelStatusChanged(payload)
            else -> throw CoreException(ErrorType.INVALID_REQUEST, "지원하지 않는 토스페이먼츠 웹훅 eventType입니다.")
        }
    }

    private fun processDepositCallback(payload: TossPaymentWebhookPayload): PaymentWebhookCommand {
        val orderId = payload.orderId?.trim().orEmpty()
        val status = payload.status?.trim().orEmpty()
        if (orderId.isBlank() || status.isBlank()) {
            throw CoreException(ErrorType.INVALID_REQUEST, "유효하지 않은 토스페이먼츠 가상계좌 콜백 페이로드입니다.")
        }

        return PaymentWebhookCommand(
            orderKey = orderId,
            paymentKey = null,
            status = status,
            method = PaymentMethod.VIRTUAL_ACCOUNT,
            cancelTransactionKey = payload.transactionKey?.trim(),
        )
    }

    private fun processPaymentStatusChanged(payload: TossPaymentWebhookPayload): PaymentWebhookCommand {
        val data = payload.data ?: throw CoreException(ErrorType.INVALID_REQUEST, "토스페이먼츠 웹훅 data 블록이 누락되었습니다.")
        val orderId = data.orderId?.trim().orEmpty()
        val paymentKey = data.paymentKey?.trim().orEmpty()
        val status = data.status?.trim().orEmpty()
        if (orderId.isBlank() || paymentKey.isBlank() || status.isBlank()) {
            throw CoreException(ErrorType.INVALID_REQUEST, "유효하지 않은 토스페이먼츠 결제 상태 변경 페이로드입니다.")
        }

        val latestCancel = data.cancels?.lastOrNull()
        val parsedApprovedAt = data.approvedAt?.let {
            runCatching { OffsetDateTime.parse(it) }.getOrNull()
        }

        return PaymentWebhookCommand(
            orderKey = orderId,
            paymentKey = paymentKey,
            status = status,
            method = tossPaymentsMethodMapper.mapOrNull(data.method, data.easyPay?.provider),
            approvedAt = parsedApprovedAt,
            cancelTransactionKey = latestCancel?.transactionKey?.trim(),
            cancelReason = latestCancel?.cancelReason?.trim(),
        )
    }

    private fun processCancelStatusChanged(payload: TossPaymentWebhookPayload): PaymentWebhookCommand {
        val data = payload.data
            ?: throw CoreException(ErrorType.INVALID_REQUEST, "토스페이먼츠 웹훅 data 블록이 누락되었습니다.")
        val orderId = data.orderId?.trim().orEmpty()
        val cancelStatus = data.cancelStatus?.trim() ?: data.status?.trim().orEmpty()
        if (orderId.isBlank() || cancelStatus.isBlank()) {
            throw CoreException(ErrorType.INVALID_REQUEST, "유효하지 않은 토스페이먼츠 취소 상태 변경 페이로드입니다.")
        }

        val mappedStatus = when (cancelStatus.uppercase()) {
            "DONE", "CANCELED" -> "CANCELED"
            else -> cancelStatus
        }

        return PaymentWebhookCommand(
            orderKey = orderId,
            paymentKey = data.paymentKey?.trim(),
            status = mappedStatus,
            method = tossPaymentsMethodMapper.mapOrNull(data.method, data.easyPay?.provider),
            cancelTransactionKey = data.transactionKey?.trim(),
            cancelReason = data.cancelReason?.trim(),
        )
    }
}
