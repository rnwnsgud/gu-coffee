package com.coffee.gu.payment.webhook.security

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class TossWebhookSecurityValidatorTest {

    private val validator = TossWebhookSecurityValidator(
        ipWhitelistEnabled = true,
        allowedIps = "203.0.113.10,10.20.0.0/16",
        signatureEnabled = true,
        webhookSecret = "toss-secret-key",
    )

    @Test
    @DisplayName("허용된 IP 대역과 유효한 v1 Base64 HMAC 서명이면 검증을 통과한다")
    fun passWhenIpAndSignatureValid() {
        val rawPayload = """{"eventType":"PAYMENT_STATUS_CHANGED","data":{"orderId":"ORDER-1"}}"""
        val transmissionTime = "2026-11-18T18:00:00.000000"
        val signature = "v1:" + TossWebhookSecurityValidator.computeHmacSha256Base64(
            secret = "toss-secret-key",
            rawPayload = rawPayload,
            transmissionTime = transmissionTime,
        )

        assertThatCode {
            validator.validate(
                clientIp = "10.20.1.5",
                rawPayload = rawPayload,
                signature = signature,
                transmissionTime = transmissionTime,
            )
        }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("허용되지 않은 IP에서 요청이 오면 FORBIDDEN 예외가 발생한다")
    fun rejectDisallowedIp() {
        val rawPayload = """{"eventType":"PAYMENT_STATUS_CHANGED"}"""

        assertThatThrownBy {
            validator.validate(
                clientIp = "198.51.100.1",
                rawPayload = rawPayload,
                signature = "v1:anything",
                transmissionTime = null,
            )
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.FORBIDDEN)
    }

    @Test
    @DisplayName("서명이 위변조되었으면 UNAUTHORIZED 예외가 발생한다")
    fun rejectTamperedSignature() {
        val rawPayload = """{"eventType":"PAYMENT_STATUS_CHANGED","data":{"orderId":"ORDER-1"}}"""
        val validSignature = "v1:" + TossWebhookSecurityValidator.computeHmacSha256Base64(
            secret = "toss-secret-key",
            rawPayload = rawPayload,
        )
        val tamperedPayload = """{"eventType":"PAYMENT_STATUS_CHANGED","data":{"orderId":"ORDER-2"}}"""

        assertThatThrownBy {
            validator.validate(
                clientIp = "203.0.113.10",
                rawPayload = tamperedPayload,
                signature = validSignature,
            )
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.UNAUTHORIZED)
    }
}
