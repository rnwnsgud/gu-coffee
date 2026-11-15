package com.coffee.gu.partner.config

import com.coffee.gu.partner.auth.PartnerPrincipalArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration(proxyBeanMethods = false)
class PartnerWebMvcConfig(
    private val partnerPrincipalArgumentResolver: PartnerPrincipalArgumentResolver,
) : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(partnerPrincipalArgumentResolver)
    }
}
