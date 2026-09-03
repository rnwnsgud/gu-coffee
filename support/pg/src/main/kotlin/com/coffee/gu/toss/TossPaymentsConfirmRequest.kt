package com.coffee.gu.toss

import java.math.BigDecimal

class TossPaymentsConfirmRequest(
    val paymentKey: String,
    val orderId: String,
    val amount: BigDecimal
)
