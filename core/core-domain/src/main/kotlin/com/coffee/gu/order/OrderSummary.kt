package com.coffee.gu.order

import com.coffee.gu.Principal
import com.coffee.gu.enums.OrderState
import java.math.BigDecimal

data class OrderSummary(
    val key: String,
    val name: String,
    val principal: Principal,
    val totalPrice: BigDecimal,
    val state: OrderState,
)