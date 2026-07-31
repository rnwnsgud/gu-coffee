package com.coffee.gu.toss;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.toss")
public record TossPaymentsProperties(
        String secretKey,
        String baseURl
) {
}
