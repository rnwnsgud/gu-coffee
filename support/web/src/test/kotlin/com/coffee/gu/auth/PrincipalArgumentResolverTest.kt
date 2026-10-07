package com.coffee.gu.auth

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.enums.PrincipalType
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.core.MethodParameter
import org.springframework.web.context.request.NativeWebRequest

class PrincipalArgumentResolverTest {

    private val resolver = PrincipalArgumentResolver()

    @Test
    @DisplayName("Principal 타입 파라미터는 supportsParameter가 true를 반환한다")
    fun testSupportsParameter() {
        val parameter = mock<MethodParameter> {
            on { parameterType } doReturn (Principal::class.java as Class<*>)
        }
        assertThat(resolver.supportsParameter(parameter)).isTrue()
    }

    @Test
    @DisplayName("유효한 USER 인증 헤더가 전달되면 정상적으로 Principal(USER) 객체가 생성된다")
    fun testResolveArgumentSuccess() {
        val httpRequest = mock<HttpServletRequest> {
            on { getHeader("Gu-Coffee-com.coffee.gu.Principal-Id") } doReturn "user-1"
            on { getHeader("Gu-Coffee-com.coffee.gu.Principal-Type") } doReturn "USER"
        }
        val webRequest = mock<NativeWebRequest> {
            on { getNativeRequest(HttpServletRequest::class.java) } doReturn httpRequest
        }
        val annotation = mock<Authenticated> {
            on { required } doReturn true
        }
        val parameter = mock<MethodParameter> {
            on { getParameterAnnotation(Authenticated::class.java) } doReturn annotation
        }

        val result = resolver.resolveArgument(parameter, null, webRequest, null)

        assertThat(result).isInstanceOf(Principal::class.java)
        val principal = result as Principal
        assertThat(principal.key).isEqualTo("user-1")
        assertThat(principal.type).isEqualTo(PrincipalType.USER)
    }

    @Test
    @DisplayName("인증 헤더(Id 또는 Type)가 누락되면 CoreException(UNAUTHORIZED)이 발생한다")
    fun testMissingHeaderThrowsUnauthorized() {
        val httpRequest = mock<HttpServletRequest> {
            on { getHeader("Gu-Coffee-com.coffee.gu.Principal-Id") } doReturn null
            on { getHeader("Gu-Coffee-com.coffee.gu.Principal-Type") } doReturn null
        }
        val webRequest = mock<NativeWebRequest> {
            on { getNativeRequest(HttpServletRequest::class.java) } doReturn httpRequest
        }
        val annotation = mock<Authenticated> {
            on { required } doReturn true
        }
        val parameter = mock<MethodParameter> {
            on { getParameterAnnotation(Authenticated::class.java) } doReturn annotation
        }

        assertThatThrownBy { resolver.resolveArgument(parameter, null, webRequest, null) }
            .isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.UNAUTHORIZED)
    }

    @Test
    @DisplayName("회원 전용 서비스에 GUEST 타입이 인입되면 UNAUTHORIZED 에러가 발생한다")
    fun testGuestAccessOnUserOnlyThrowsUnauthorized() {
        val httpRequest = mock<HttpServletRequest> {
            on { getHeader("Gu-Coffee-com.coffee.gu.Principal-Id") } doReturn "guest-1"
            on { getHeader("Gu-Coffee-com.coffee.gu.Principal-Type") } doReturn "GUEST"
        }
        val webRequest = mock<NativeWebRequest> {
            on { getNativeRequest(HttpServletRequest::class.java) } doReturn httpRequest
        }
        val annotation = mock<Authenticated> {
            on { required } doReturn true
        }
        val parameter = mock<MethodParameter> {
            on { getParameterAnnotation(Authenticated::class.java) } doReturn annotation
        }

        assertThatThrownBy { resolver.resolveArgument(parameter, null, webRequest, null) }
            .isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.UNAUTHORIZED)
    }
}
