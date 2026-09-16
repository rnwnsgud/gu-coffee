package com.coffee.gu

class PaymentApprovedEvent(
    val orderKey: String,
    val hasAppliedCoupon: Boolean,
)
