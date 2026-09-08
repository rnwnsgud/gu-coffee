package com.coffee.gu.cart

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.order.NewOrder
import com.coffee.gu.order.NewOrderLine
import java.math.BigDecimal

class Cart(
    val principal: Principal,
    val items: List<CartItem>,
) {
    fun toNewOrder(targetItemIds: Set<Long>, storeId: Long): NewOrder {
        if (items.isEmpty()) throw CoreException(ErrorType.INVALID_REQUEST, null)

        val lines = items.filter { targetItemIds.contains(it.id) }
            .map { item ->
                NewOrderLine(
                    menuId = item.menu?.id ?: 0L,
                    quantity = item.quantity,
                    unitPrice = item.menu?.price?.salesPrice ?: BigDecimal.ZERO,
                    isStampEligible = true,
                )
            }

        return NewOrder(
            principal = principal,
            storeId = storeId,
            lines = lines,
        )
    }
}
