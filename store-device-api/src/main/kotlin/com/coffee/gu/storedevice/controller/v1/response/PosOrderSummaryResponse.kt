package com.coffee.gu.storedevice.controller.v1.response

import com.coffee.gu.enums.OrderChannel
import com.coffee.gu.enums.OrderState
import com.coffee.gu.order.Order
import java.math.BigDecimal

data class PosOrderSummaryResponse(
    val orderKey: String,
    val name: String,
    val storeId: Long,
    val totalPrice: BigDecimal,
    val state: OrderState,
    val channel: OrderChannel,
) {
    companion object {
        fun from(order: Order): PosOrderSummaryResponse {
            return PosOrderSummaryResponse(
                orderKey = order.key,
                name = order.name,
                storeId = order.storeId,
                totalPrice = order.totalPrice,
                state = order.state,
                channel = order.channel,
            )
        }
    }
}
