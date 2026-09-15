package com.coffee.gu.order

import java.math.BigDecimal

data class NewOrderLine(
    val menuId: Long,
    val quantity: Long,
    val unitPrice: BigDecimal = BigDecimal.ZERO,
    val isStampEligible: Boolean = true,
) {
    constructor(menuId: Long, quantity: Long) : this(menuId, quantity, BigDecimal.ZERO, true)
}
