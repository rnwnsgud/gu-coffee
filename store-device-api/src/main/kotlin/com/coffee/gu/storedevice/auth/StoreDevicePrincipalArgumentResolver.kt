package com.coffee.gu.storedevice.auth

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class StoreDevicePrincipalArgumentResolver(
    private val storeDeviceAuthenticator: StoreDeviceAuthenticator,
) : HandlerMethodArgumentResolver {

    companion object {
        const val DEVICE_API_KEY_HEADER = "Gu-Coffee-Device-Api-Key"
    }

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(StoreDeviceAuthenticated::class.java) &&
            parameter.parameterType.isAssignableFrom(StoreDevicePrincipal::class.java)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any {
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java)
            ?: throw CoreException(ErrorType.INVALID_REQUEST, null)

        val apiKey = request.getHeader(DEVICE_API_KEY_HEADER)
        return storeDeviceAuthenticator.authenticate(apiKey)
    }
}
