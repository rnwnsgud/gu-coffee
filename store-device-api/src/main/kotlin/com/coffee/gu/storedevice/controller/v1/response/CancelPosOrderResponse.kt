package com.coffee.gu.storedevice.controller.v1.response

import com.coffee.gu.enums.OrderState
import com.coffee.gu.order.Order

data class CancelPosOrderResponse(
    val orderKey: String,
    val state: OrderState,
) {
    companion object {
        fun from(order: Order): CancelPosOrderResponse {
            return CancelPosOrderResponse(
                orderKey = order.key,
                state = order.state,
            )
        }
    }
}
