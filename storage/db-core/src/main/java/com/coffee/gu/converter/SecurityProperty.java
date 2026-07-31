package com.coffee.gu.converter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gu-coffee.storage.core.security")
public record SecurityProperty(
        String key,
        String iv
) {}
