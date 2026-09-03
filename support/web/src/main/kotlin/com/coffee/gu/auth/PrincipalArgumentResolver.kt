package com.coffee.gu.auth

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.enums.PrincipalType
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class PrincipalArgumentResolver : HandlerMethodArgumentResolver {

    companion object {
        private const val PRINCIPAL_ID_HEADER = "Gu-Coffee-com.coffee.gu.Principal-Id"
        private const val PRINCIPAL_TYPE_HEADER = "Gu-Coffee-com.coffee.gu.Principal-Type"
    }

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.parameterType.isAssignableFrom(Principal::class.java)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Any {
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java)
            ?: throw CoreException(ErrorType.INVALID_REQUEST, null)

        val annotation = parameter.getParameterAnnotation(Authenticated::class.java)

        val id = request.getHeader(PRINCIPAL_ID_HEADER)
        val type = request.getHeader(PRINCIPAL_TYPE_HEADER)

        validatePrincipal(annotation, id, type)

        return Principal(id!!, type!!)
    }

    private fun validatePrincipal(annotation: Authenticated?, id: String?, type: String?) {
        val isRequired = annotation != null && annotation.required

        if (isRequired) {
            if (id == null) throw CoreException(ErrorType.UNAUTHORIZED, null)
            if (PrincipalType.USER.name != type) {
                throw CoreException(ErrorType.UNAUTHORIZED, "회원 전용 서비스입니다.")
            }
        }
        if (type == null) {
            throw CoreException(ErrorType.INVALID_REQUEST, null)
        }
    }
}
