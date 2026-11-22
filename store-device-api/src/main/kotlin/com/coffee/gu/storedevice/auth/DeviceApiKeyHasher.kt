package com.coffee.gu.storedevice.auth

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object DeviceApiKeyHasher {

    fun sha256Hex(rawApiKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(rawApiKey.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun matches(expectedHash: String, actualHash: String): Boolean {
        return MessageDigest.isEqual(
            expectedHash.toByteArray(StandardCharsets.UTF_8),
            actualHash.toByteArray(StandardCharsets.UTF_8),
        )
    }
}
