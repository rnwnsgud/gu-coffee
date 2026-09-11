package com.coffee.gu.api.controller.v1.request

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.cart.AddCartItem

class AddCartItemRequest(
    val menuId: Long,
    val quantity: Long,
) {
    fun toAddCartItem(): AddCartItem {
        if (quantity <= 0) throw CoreException(ErrorType.INVALID_REQUEST, null)
        return AddCartItem(menuId, quantity)
    }
}
