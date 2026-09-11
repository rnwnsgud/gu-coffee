package com.coffee.gu.api.controller.v1.request

import com.coffee.gu.cart.ModifyCartItem

class ModifyCartItemRequest(
    val quantity: Long,
) {
    fun toModifyCartItem(cartItemId: Long): ModifyCartItem {
        return ModifyCartItem(cartItemId, quantity)
    }
}
