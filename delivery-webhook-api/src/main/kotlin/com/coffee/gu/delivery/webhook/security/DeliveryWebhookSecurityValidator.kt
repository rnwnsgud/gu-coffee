package com.coffee.gu.delivery.webhook.security

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.auth.HmacSignatureVerifier
import com.coffee.gu.auth.IpWhitelistMatcher
import com.coffee.gu.enums.DeliveryPlatform
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class DeliveryWebhookSecurityValidator(
    @param:Value("\${security.delivery-webhook.ip-whitelist.enabled:false}")
    private val ipWhitelistEnabled: Boolean = false,
    @param:Value("\${security.delivery-webhook.ip-whitelist.baemin:127.0.0.1,::1}")
    private val baeminAllowedIps: String = "127.0.0.1,::1",
    @param:Value("\${security.delivery-webhook.ip-whitelist.coupang-eats:127.0.0.1,::1}")
    private val coupangEatsAllowedIps: String = "127.0.0.1,::1",
    @param:Value("\${security.delivery-webhook.signature.enabled:false}")
    private val signatureEnabled: Boolean = false,
    @param:Value("\${security.delivery-webhook.signature.baemin-secret:local-baemin-webhook-secret}")
    private val baeminSecret: String = "local-baemin-webhook-secret",
    @param:Value("\${security.delivery-webhook.signature.coupang-eats-secret:local-coupang-eats-webhook-secret}")
    private val coupangEatsSecret: String = "local-coupang-eats-webhook-secret",
) {
    companion object {
        fun computeHmacSha256Hex(secret: String, rawPayload: String): String {
            return HmacSignatureVerifier.computeHmacSha256Hex(secret, rawPayload)
        }
    }

    fun validate(
        platform: DeliveryPlatform,
        clientIp: String?,
        rawPayload: String,
        signature: String?,
    ) {
        if (ipWhitelistEnabled) {
            validateClientIp(platform, clientIp)
        }
        if (signatureEnabled) {
            validateHmacSignature(platform, rawPayload, signature)
        }
    }

    private fun validateClientIp(platform: DeliveryPlatform, clientIp: String?) {
        if (clientIp.isNullOrBlank()) {
            throw CoreException(ErrorType.FORBIDDEN, "웹훅 발신 IP를 확인할 수 없습니다.")
        }

        val rawAllowedList = when (platform) {
            DeliveryPlatform.BAEMIN -> baeminAllowedIps
            DeliveryPlatform.COUPANG_EATS -> coupangEatsAllowedIps
        }

        if (!IpWhitelistMatcher.isAllowed(clientIp, rawAllowedList)) {
            throw CoreException(ErrorType.FORBIDDEN, "허용되지 않은 웹훅 발신 IP입니다.")
        }
    }

    private fun validateHmacSignature(
        platform: DeliveryPlatform,
        rawPayload: String,
        signature: String?,
    ) {
        val secret = when (platform) {
            DeliveryPlatform.BAEMIN -> baeminSecret
            DeliveryPlatform.COUPANG_EATS -> coupangEatsSecret
        }
        if (secret.isBlank()) {
            throw CoreException(ErrorType.UNAUTHORIZED, "플랫폼 웹훅 서명 시크릿이 설정되지 않았습니다.")
        }
        if (signature.isNullOrBlank()) {
            throw CoreException(ErrorType.UNAUTHORIZED, "웹훅 서명 헤더가 누락되었습니다.")
        }

        val normalizedSignature = signature.trim().removePrefix("sha256=").lowercase()
        val expectedSignature = computeHmacSha256Hex(secret, rawPayload)

        if (!HmacSignatureVerifier.constantTimeEquals(normalizedSignature, expectedSignature)) {
            throw CoreException(ErrorType.UNAUTHORIZED, "유효하지 않은 웹훅 서명입니다.")
        }
    }
}
