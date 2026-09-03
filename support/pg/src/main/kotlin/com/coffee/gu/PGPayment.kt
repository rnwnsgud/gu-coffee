package com.coffee.gu

import java.math.BigDecimal

class PGPayment(
    val paymentKey: String,
    val orderKey: String,
    val amount: BigDecimal,
    val status: PaymentGatewayStatus
)
