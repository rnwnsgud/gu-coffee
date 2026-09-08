package com.coffee.gu.cart

@JvmRecord
data class AddCartItem(
    val menuId: Long,
    val quantity: Long,
)
