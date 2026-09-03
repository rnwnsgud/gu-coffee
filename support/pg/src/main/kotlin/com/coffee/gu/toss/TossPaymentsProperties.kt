package com.coffee.gu.toss

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "payment.toss")
class TossPaymentsProperties(
    val secretKey: String,
    val baseUrl: String
)
