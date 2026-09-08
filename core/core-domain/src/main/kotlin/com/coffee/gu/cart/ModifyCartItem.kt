package com.coffee.gu.cart

@JvmRecord
data class ModifyCartItem(
    val cartItemId: Long,
    val quantity: Long,
)
