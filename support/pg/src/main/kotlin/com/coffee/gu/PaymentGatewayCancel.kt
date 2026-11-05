package com.coffee.gu

data class PaymentGatewayCancel(
    val paymentKey: String,
    val cancelReason: String,
)
