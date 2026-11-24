package com.coffee.gu.payment.webhook.security

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.auth.HmacSignatureVerifier
import com.coffee.gu.auth.IpWhitelistMatcher
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TossWebhookSecurityValidator(
    @param:Value("\${security.payment-webhook.toss.ip-whitelist.enabled:false}")
    private val ipWhitelistEnabled: Boolean = false,
    @param:Value("\${security.payment-webhook.toss.ip-whitelist.allowed-ips:127.0.0.1,::1}")
    private val allowedIps: String = "127.0.0.1,::1",
    @param:Value("\${security.payment-webhook.toss.signature.enabled:false}")
    private val signatureEnabled: Boolean = false,
    @param:Value("\${security.payment-webhook.toss.signature.secret:local-toss-webhook-secret}")
    private val webhookSecret: String = "local-toss-webhook-secret",
) {
    companion object {
        fun computeHmacSha256Base64(
            secret: String,
            rawPayload: String,
            transmissionTime: String? = null,
        ): String {
            val message = if (transmissionTime.isNullOrBlank()) {
                rawPayload
            } else {
                "$rawPayload:$transmissionTime"
            }
            return HmacSignatureVerifier.computeHmacSha256Base64(secret, message)
        }
    }

    fun validate(
        clientIp: String?,
        rawPayload: String,
        signature: String?,
        transmissionTime: String? = null,
    ) {
        if (ipWhitelistEnabled) {
            validateClientIp(clientIp)
        }
        if (signatureEnabled) {
            validateSignature(rawPayload, signature, transmissionTime)
        }
    }

    private fun validateClientIp(clientIp: String?) {
        if (clientIp.isNullOrBlank()) {
            throw CoreException(ErrorType.FORBIDDEN, "토스 웹훅 발신 IP를 확인할 수 없습니다.")
        }
        if (!IpWhitelistMatcher.isAllowed(clientIp, allowedIps)) {
            throw CoreException(ErrorType.FORBIDDEN, "허용되지 않은 토스 웹훅 발신 IP입니다.")
        }
    }

    private fun validateSignature(
        rawPayload: String,
        signature: String?,
        transmissionTime: String?,
    ) {
        if (webhookSecret.isBlank()) {
            throw CoreException(ErrorType.UNAUTHORIZED, "토스 웹훅 시크릿이 설정되지 않았습니다.")
        }
        if (signature.isNullOrBlank()) {
            throw CoreException(ErrorType.UNAUTHORIZED, "토스 웹훅 서명 헤더가 누락되었습니다.")
        }

        val expectedBase64 = computeHmacSha256Base64(webhookSecret, rawPayload, transmissionTime)
        val candidateTokens = signature.split(",")
            .map { it.trim().removePrefix("v1:") }
            .filter { it.isNotEmpty() }

        val isValid = candidateTokens.any { token ->
            HmacSignatureVerifier.constantTimeEquals(token, expectedBase64)
        }
        if (!isValid) {
            throw CoreException(ErrorType.UNAUTHORIZED, "유효하지 않은 토스 웹훅 서명입니다.")
        }
    }
}
