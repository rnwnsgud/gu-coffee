package com.coffee.gu.partner.auth

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.auth.GatewayInternalTokenValidator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.ServletWebRequest

class PartnerPrincipalArgumentResolverTest {

    private val resolver = PartnerPrincipalArgumentResolver()

    @Suppress("UNUSED_PARAMETER")
    private fun sampleEndpoint(@PartnerAuthenticated partnerPrincipal: PartnerPrincipal) = Unit

    private fun methodParameter(): MethodParameter {
        val method = this::class.java.getDeclaredMethod("sampleEndpoint", PartnerPrincipal::class.java)
        return MethodParameter(method, 0)
    }

    @Test
    @DisplayName("유효한 파트너 인증 헤더가 전달되면 PartnerPrincipal을 생성하여 반환한다")
    fun resolveValidPartnerPrincipal() {
        val request = MockHttpServletRequest().apply {
            addHeader(PartnerPrincipalArgumentResolver.PARTNER_ID_HEADER, "PARTNER-1")
            addHeader(PartnerPrincipalArgumentResolver.PARTNER_STORE_ID_HEADER, "1")
        }

        val result = resolver.resolveArgument(methodParameter(), null, ServletWebRequest(request), null) as PartnerPrincipal

        assertThat(result.partnerId).isEqualTo("PARTNER-1")
        assertThat(result.storeId).isEqualTo(1L)
    }

    @Test
    @DisplayName("파트너 인증 헤더가 누락되면 UNAUTHORIZED 예외가 발생한다")
    fun throwUnauthorizedWhenHeaderMissing() {
        val request = MockHttpServletRequest()

        assertThatThrownBy {
            resolver.resolveArgument(methodParameter(), null, ServletWebRequest(request), null)
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.UNAUTHORIZED)
    }

    @Test
    @DisplayName("매장 식별자 헤더가 숫자가 아니거나 0 이하이면 INVALID_REQUEST 예외가 발생한다")
    fun throwInvalidRequestWhenStoreIdInvalid() {
        val request = MockHttpServletRequest().apply {
            addHeader(PartnerPrincipalArgumentResolver.PARTNER_ID_HEADER, "PARTNER-1")
            addHeader(PartnerPrincipalArgumentResolver.PARTNER_STORE_ID_HEADER, "invalid")
        }

        assertThatThrownBy {
            resolver.resolveArgument(methodParameter(), null, ServletWebRequest(request), null)
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_REQUEST)
    }

    @Test
    @DisplayName("내부 토큰 검증이 활성화된 상태에서 Gu-Coffee-Internal-Token이 없거나 불일치하면 파트너 헤더가 있어도 UNAUTHORIZED 예외가 발생한다")
    fun throwUnauthorizedWhenGatewayInternalTokenInvalid() {
        val strictResolver = PartnerPrincipalArgumentResolver(
            GatewayInternalTokenValidator(enabled = true, secret = "internal-secret-123"),
        )
        val request = MockHttpServletRequest().apply {
            addHeader(PartnerPrincipalArgumentResolver.PARTNER_ID_HEADER, "PARTNER-1")
            addHeader(PartnerPrincipalArgumentResolver.PARTNER_STORE_ID_HEADER, "1")
        }

        assertThatThrownBy {
            strictResolver.resolveArgument(methodParameter(), null, ServletWebRequest(request), null)
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.UNAUTHORIZED)
    }
}
