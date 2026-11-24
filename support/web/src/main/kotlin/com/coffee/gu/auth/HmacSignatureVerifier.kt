package com.coffee.gu.auth

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacSignatureVerifier {

    private const val HMAC_SHA256 = "HmacSHA256"

    fun computeHmacSha256Hex(secret: String, message: String): String {
        val rawHmac = computeRawHmacSha256(secret, message)
        return rawHmac.joinToString("") { "%02x".format(it) }
    }

    fun computeHmacSha256Base64(secret: String, message: String): String {
        val rawHmac = computeRawHmacSha256(secret, message)
        return Base64.getEncoder().encodeToString(rawHmac)
    }

    fun constantTimeEquals(actual: String, expected: String): Boolean {
        return MessageDigest.isEqual(
            actual.toByteArray(StandardCharsets.UTF_8),
            expected.toByteArray(StandardCharsets.UTF_8),
        )
    }

    private fun computeRawHmacSha256(secret: String, message: String): ByteArray {
        val mac = Mac.getInstance(HMAC_SHA256)
        val keySpec = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), HMAC_SHA256)
        mac.init(keySpec)
        return mac.doFinal(message.toByteArray(StandardCharsets.UTF_8))
    }
}
