package com.coffee.gu.config

import com.coffee.gu.auth.PrincipalArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration(proxyBeanMethods = false)
open class WebMvcConfig(
    private val principalArgumentResolver: PrincipalArgumentResolver,
) : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(principalArgumentResolver)
    }
}
