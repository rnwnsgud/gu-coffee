package com.coffee.gu.auth

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Component
class GatewayInternalTokenValidator(
    @param:Value("\${security.gateway.internal-token.enabled:false}")
    private val enabled: Boolean = false,
    @param:Value("\${security.gateway.internal-token.secret:}")
    private val secret: String = "",
) {
    companion object {
        const val INTERNAL_TOKEN_HEADER = "Gu-Coffee-Internal-Token"
    }

    fun validate(request: HttpServletRequest) {
        if (!enabled) return

        if (secret.isBlank()) {
            throw CoreException(ErrorType.UNAUTHORIZED, "게이트웨이 내부 인증 시크릿이 설정되지 않았습니다.")
        }

        val token = request.getHeader(INTERNAL_TOKEN_HEADER)
        if (token.isNullOrBlank() || !MessageDigest.isEqual(
                token.toByteArray(StandardCharsets.UTF_8),
                secret.toByteArray(StandardCharsets.UTF_8),
            )
        ) {
            throw CoreException(ErrorType.UNAUTHORIZED, "유효하지 않은 게이트웨이 내부 토큰입니다.")
        }
    }
}
