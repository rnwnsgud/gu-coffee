package com.coffee.gu.api.controller.v1.response.order

import com.coffee.gu.enums.OrderState
import com.coffee.gu.order.OrderSummary
import java.math.BigDecimal

class OrderListResponse(
    val key: String,
    val name: String,
    val totalPrice: BigDecimal,
    val state: OrderState,
) {
    companion object {
        @JvmStatic
        fun from(order: OrderSummary): OrderListResponse {
            return OrderListResponse(
                key = order.key,
                name = order.name,
                totalPrice = order.totalPrice,
                state = order.state
            )
        }

        @JvmStatic
        fun from(orders: List<OrderSummary>): List<OrderListResponse> {
            return orders.map { from(it) }
        }
    }
}
