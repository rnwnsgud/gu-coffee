package com.coffee.gu.api.controller.v1.response.order

import com.coffee.gu.enums.OrderState
import com.coffee.gu.order.Order
import java.math.BigDecimal

class OrderResponse(
    val key: String,
    val name: String,
    val totalPrice: BigDecimal,
    val state: OrderState,
    val lines: List<OrderLineResponse>,
) {
    companion object {
        fun from(order: Order): OrderResponse {
            return OrderResponse(
                key = order.key,
                name = order.name,
                totalPrice = order.totalPrice,
                state = order.state,
                lines = order.lines.map { line ->
                    OrderLineResponse(
                        menuId = line.menuId,
                        menuName = line.menuName,
                        imageUrl = line.imageUrl ?: "",
                        description = line.description ?: "",
                        quantity = line.quantity,
                        unitPrice = line.unitPrice,
                        totalPrice = line.totalPrice
                    )
                }
            )
        }
    }
}
