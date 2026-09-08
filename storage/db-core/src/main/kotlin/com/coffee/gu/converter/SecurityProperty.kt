package com.coffee.gu.converter

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "gu-coffee.storage.core.security")
class SecurityProperty(
    val key: String,
    val iv: String,
)
