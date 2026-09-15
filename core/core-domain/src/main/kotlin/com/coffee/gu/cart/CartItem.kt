package com.coffee.gu.cart

import com.coffee.gu.menu.Menu

class CartItem(
    val id: Long = 0,
    val menu: Menu,
    var quantity: Long = 0,
    var isDeleted: Boolean = false,
) {
    fun applyQuantity(quantity: Long) {
        this.quantity = if (quantity < 1) 1 else quantity
    }

    fun delete() {
        this.isDeleted = true
    }

    fun active() {
        this.isDeleted = false
    }

}
