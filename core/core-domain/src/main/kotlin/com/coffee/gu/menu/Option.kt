package com.coffee.gu.menu

import java.math.BigDecimal

class Option(
    val id: Long = 0,
    val optionGroupId: Long,
    val name: String,
    val extraPrice: BigDecimal,
)