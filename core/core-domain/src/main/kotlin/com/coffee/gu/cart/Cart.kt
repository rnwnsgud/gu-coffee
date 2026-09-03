package com.coffee.gu.cart

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.order.NewOrder
import com.coffee.gu.order.NewOrderLine

class Cart(
    val principal: Principal,
    val items: List<CartItem>,
) {
    fun toNewOrder(targetItemIds: Set<Long>, storeId: Long): NewOrder {
        if (items.isEmpty()) throw CoreException(ErrorType.INVALID_REQUEST, null)

        val lines = items.filter { targetItemIds.contains(it.id) }
            .map { item ->
                NewOrderLine(
                    menuId = item.menu.id,
                    quantity = item.quantity
                )
            }

        return NewOrder(
            principal = principal,
            storeId = storeId,
            lines = lines
        )
    }
}
