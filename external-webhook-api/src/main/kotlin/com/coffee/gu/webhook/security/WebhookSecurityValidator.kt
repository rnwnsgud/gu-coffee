package com.coffee.gu.webhook.security

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.enums.DeliveryPlatform
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class WebhookSecurityValidator(
    @param:Value("\${security.webhook.ip-whitelist.enabled:false}")
    private val ipWhitelistEnabled: Boolean = false,
    @param:Value("\${security.webhook.ip-whitelist.baemin:127.0.0.1,::1}")
    private val baeminAllowedIps: String = "127.0.0.1,::1",
    @param:Value("\${security.webhook.ip-whitelist.coupang-eats:127.0.0.1,::1}")
    private val coupangEatsAllowedIps: String = "127.0.0.1,::1",
    @param:Value("\${security.webhook.signature.enabled:false}")
    private val signatureEnabled: Boolean = false,
    @param:Value("\${security.webhook.signature.baemin-secret:local-baemin-webhook-secret}")
    private val baeminSecret: String = "local-baemin-webhook-secret",
    @param:Value("\${security.webhook.signature.coupang-eats-secret:local-coupang-eats-webhook-secret}")
    private val coupangEatsSecret: String = "local-coupang-eats-webhook-secret",
) {
    companion object {
        private const val HMAC_SHA256 = "HmacSHA256"

        fun computeHmacSha256Hex(secret: String, rawPayload: String): String {
            val mac = Mac.getInstance(HMAC_SHA256)
            val keySpec = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), HMAC_SHA256)
            mac.init(keySpec)
            val rawHmac = mac.doFinal(rawPayload.toByteArray(StandardCharsets.UTF_8))
            return rawHmac.joinToString("") { "%02x".format(it) }
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
        val normalizedClientIp = clientIp?.trim()
        if (normalizedClientIp.isNullOrBlank()) {
            throw CoreException(ErrorType.FORBIDDEN, "웹훅 발신 IP를 확인할 수 없습니다.")
        }

        val rawAllowedList = when (platform) {
            DeliveryPlatform.BAEMIN -> baeminAllowedIps
            DeliveryPlatform.COUPANG_EATS -> coupangEatsAllowedIps
        }

        val allowedEntries = rawAllowedList.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val isAllowed = allowedEntries.any { rule -> matchesIpRule(normalizedClientIp, rule) }
        if (!isAllowed) {
            throw CoreException(ErrorType.FORBIDDEN, "허용되지 않은 웹훅 발신 IP입니다.")
        }
    }

    private fun matchesIpRule(clientIp: String, rule: String): Boolean {
        return try {
            if (!rule.contains("/")) {
                InetAddress.getByName(clientIp) == InetAddress.getByName(rule)
            } else {
                val parts = rule.split("/")
                val networkAddress = InetAddress.getByName(parts[0]).address
                val targetAddress = InetAddress.getByName(clientIp).address
                if (networkAddress.size != targetAddress.size) return false

                val prefixLength = parts[1].toInt()
                val fullBytes = prefixLength / 8
                val remainderBits = prefixLength % 8

                for (i in 0 until fullBytes) {
                    if (networkAddress[i] != targetAddress[i]) return false
                }
                if (remainderBits > 0) {
                    val mask = (0xFF shl (8 - remainderBits)) and 0xFF
                    if ((networkAddress[fullBytes].toInt() and mask) != (targetAddress[fullBytes].toInt() and mask)) {
                        return false
                    }
                }
                true
            }
        } catch (_: Exception) {
            false
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

        val isValid = MessageDigest.isEqual(
            normalizedSignature.toByteArray(StandardCharsets.UTF_8),
            expectedSignature.toByteArray(StandardCharsets.UTF_8),
        )
        if (!isValid) {
            throw CoreException(ErrorType.UNAUTHORIZED, "유효하지 않은 웹훅 서명입니다.")
        }
    }
}
