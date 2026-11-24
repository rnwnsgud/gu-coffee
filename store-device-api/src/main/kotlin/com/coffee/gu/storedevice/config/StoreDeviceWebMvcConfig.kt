package com.coffee.gu.storedevice.config

import com.coffee.gu.storedevice.auth.StoreDevicePrincipalArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration(proxyBeanMethods = false)
class StoreDeviceWebMvcConfig(
    private val storeDevicePrincipalArgumentResolver: StoreDevicePrincipalArgumentResolver,
) : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(storeDevicePrincipalArgumentResolver)
    }
}
