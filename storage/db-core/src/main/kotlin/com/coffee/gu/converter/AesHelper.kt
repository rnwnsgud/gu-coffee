package com.coffee.gu.converter

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class AesHelper(
    private val property: SecurityProperty,
) {
    fun encrypt(target: String): String {
        return try {
            val key = SecretKeySpec(property.key.toByteArray(StandardCharsets.UTF_8), ALGORITHM)
            val iv = IvParameterSpec(property.iv.toByteArray(StandardCharsets.UTF_8))
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key, iv)
            val encrypted = cipher.doFinal(target.toByteArray(StandardCharsets.UTF_8))
            Base64.getEncoder().encodeToString(encrypted)
        } catch (e: Exception) {
            log.error("AesHelper.encrypt Exception: {}", e.message, e)
            target
        }
    }

    fun decrypt(target: String): String {
        return try {
            val key = SecretKeySpec(property.key.toByteArray(StandardCharsets.UTF_8), ALGORITHM)
            val iv = IvParameterSpec(property.iv.toByteArray(StandardCharsets.UTF_8))
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, iv)
            val decrypted = cipher.doFinal(Base64.getDecoder().decode(target))
            String(decrypted, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            log.error("AesHelper.decrypt Exception: {}", e.message, e)
            target
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AesHelper::class.java)
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    }
}
