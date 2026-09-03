package com.coffee.gu

import java.math.BigDecimal

class PaymentGatewayConfirm(
    val paymentKey: String,
    val orderKey: String,
    val amount: BigDecimal
)
