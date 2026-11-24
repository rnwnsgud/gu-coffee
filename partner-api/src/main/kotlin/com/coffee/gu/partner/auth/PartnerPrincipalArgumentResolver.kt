package com.coffee.gu.partner.auth

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.auth.GatewayInternalTokenValidator
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class PartnerPrincipalArgumentResolver(
    private val gatewayInternalTokenValidator: GatewayInternalTokenValidator = GatewayInternalTokenValidator(),
) : HandlerMethodArgumentResolver {

    companion object {
        const val PARTNER_ID_HEADER = "Gu-Coffee-Partner-Id"
        const val PARTNER_STORE_ID_HEADER = "Gu-Coffee-Partner-Store-Id"
    }

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(PartnerAuthenticated::class.java) &&
            parameter.parameterType.isAssignableFrom(PartnerPrincipal::class.java)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any {
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java)
            ?: throw CoreException(ErrorType.INVALID_REQUEST, null)

        gatewayInternalTokenValidator.validate(request)

        val partnerId = request.getHeader(PARTNER_ID_HEADER)
        val rawStoreId = request.getHeader(PARTNER_STORE_ID_HEADER)

        if (partnerId.isNullOrBlank() || rawStoreId.isNullOrBlank()) {
            throw CoreException(ErrorType.UNAUTHORIZED, "파트너 인증 헤더가 누락되었습니다.")
        }

        val storeId = rawStoreId.toLongOrNull()
        if (storeId == null || storeId <= 0) {
            throw CoreException(ErrorType.INVALID_REQUEST, "유효하지 않은 매장 식별자입니다.")
        }

        return PartnerPrincipal(
            partnerId = partnerId,
            storeId = storeId,
        )
    }
}
