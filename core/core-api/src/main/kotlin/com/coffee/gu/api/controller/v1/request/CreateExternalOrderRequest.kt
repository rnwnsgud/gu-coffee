package com.coffee.gu.api.controller.v1.request

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.enums.OrderChannel
import com.coffee.gu.order.NewOrder
import com.coffee.gu.order.NewOrderLine

data class CreateExternalOrderLineRequest(
    val menuId: Long,
    val quantity: Long,
)

data class CreateExternalOrderRequest(
    val storeId: Long,
    val channel: OrderChannel,
    val externalCustomerKey: String,
    val lines: List<CreateExternalOrderLineRequest>,
) {
    fun toNewOrder(): NewOrder {
        if (lines.isEmpty() || lines.any { it.quantity <= 0 }) {
            throw CoreException(ErrorType.INVALID_REQUEST, null)
        }
        return NewOrder(
            principal = Principal.guest(externalCustomerKey),
            storeId = storeId,
            lines = lines.map { NewOrderLine(menuId = it.menuId, quantity = it.quantity) },
            channel = channel,
        )
    }
}
