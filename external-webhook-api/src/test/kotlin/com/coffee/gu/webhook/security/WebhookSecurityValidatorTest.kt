package com.coffee.gu.webhook.security

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.enums.DeliveryPlatform
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class WebhookSecurityValidatorTest {

    private val baeminSecret = "baemin-test-secret-key"
    private val coupangSecret = "coupang-test-secret-key"

    private val validator = WebhookSecurityValidator(
        ipWhitelistEnabled = true,
        baeminAllowedIps = "203.0.113.10,10.20.0.0/16",
        coupangEatsAllowedIps = "198.51.100.0/24",
        signatureEnabled = true,
        baeminSecret = baeminSecret,
        coupangEatsSecret = coupangSecret,
    )

    @Test
    @DisplayName("허용된 단일 IP 또는 CIDR 대역에서 유효한 HMAC 서명으로 요청하면 검증을 통과한다")
    fun passWhenIpAndHmacSignatureAreValid() {
        val baeminPayload = """{"orderNo":"BM-100","shopNo":1,"memberKey":"M1","menus":[{"menuCode":"BM-LATTE","count":1}]}"""
        val baeminSig = WebhookSecurityValidator.computeHmacSha256Hex(baeminSecret, baeminPayload)

        assertThatCode {
            validator.validate(
                platform = DeliveryPlatform.BAEMIN,
                clientIp = "10.20.55.99",
                rawPayload = baeminPayload,
                signature = "sha256=$baeminSig",
            )
        }.doesNotThrowAnyException()

        val coupangPayload = """{"externalOrderId":"CE-200","merchantStoreId":1,"buyerId":"B1","orderItems":[{"productCode":"CE-LATTE","qty":2}]}"""
        val coupangSig = WebhookSecurityValidator.computeHmacSha256Hex(coupangSecret, coupangPayload)

        assertThatCode {
            validator.validate(
                platform = DeliveryPlatform.COUPANG_EATS,
                clientIp = "198.51.100.42",
                rawPayload = coupangPayload,
                signature = coupangSig,
            )
        }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("플랫폼별 화이트리스트에 등록되지 않은 IP에서 요청하면 FORBIDDEN 예외가 발생한다")
    fun throwForbiddenWhenClientIpNotAllowed() {
        val payload = """{"orderNo":"BM-100"}"""
        val validSig = WebhookSecurityValidator.computeHmacSha256Hex(baeminSecret, payload)

        assertThatThrownBy {
            validator.validate(
                platform = DeliveryPlatform.BAEMIN,
                clientIp = "198.51.100.42", // 쿠팡이츠 대역 IP로 배민 웹훅 호출
                rawPayload = payload,
                signature = validSig,
            )
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.FORBIDDEN)
    }

    @Test
    @DisplayName("페이로드가 변조되었거나 타 플랫폼 시크릿으로 서명된 경우 UNAUTHORIZED 예외가 발생한다")
    fun throwUnauthorizedWhenSignatureInvalidOrTampered() {
        val originalPayload = """{"orderNo":"BM-100","shopNo":1}"""
        val tamperedPayload = """{"orderNo":"BM-100","shopNo":2}"""
        val signatureForOriginal = WebhookSecurityValidator.computeHmacSha256Hex(baeminSecret, originalPayload)

        assertThatThrownBy {
            validator.validate(
                platform = DeliveryPlatform.BAEMIN,
                clientIp = "203.0.113.10",
                rawPayload = tamperedPayload,
                signature = signatureForOriginal,
            )
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.UNAUTHORIZED)
    }
}
