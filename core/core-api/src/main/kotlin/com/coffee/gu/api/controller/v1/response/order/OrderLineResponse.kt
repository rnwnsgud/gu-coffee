package com.coffee.gu.api.controller.v1.response.order

import java.math.BigDecimal

class OrderLineResponse(
    val menuId: Long,
    val menuName: String,
    val imageUrl: String,
    val description: String,
    val quantity: Long,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal,
)
