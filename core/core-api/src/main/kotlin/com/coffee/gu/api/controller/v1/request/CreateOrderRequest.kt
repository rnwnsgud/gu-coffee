package com.coffee.gu.api.controller.v1.request

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.order.NewOrder
import com.coffee.gu.order.NewOrderLine

class CreateOrderRequest(
    val menuId: Long,
    val quantity: Long,
    val storeId: Long,
) {
    fun toNewOrder(principal: Principal): NewOrder {
        if (quantity <= 0) throw CoreException(ErrorType.INVALID_REQUEST, null)
        return NewOrder(principal, storeId, listOf(NewOrderLine(menuId, quantity)))
    }
}
