package com.coffee.gu.storedevice.pos

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.enums.OrderState
import com.coffee.gu.order.ExternalOrderCanceler
import com.coffee.gu.order.Order
import com.coffee.gu.order.OrderRepository
import com.coffee.gu.storedevice.auth.StoreDevicePrincipal
import org.springframework.stereotype.Component

@Component
class PosOrderManager(
    private val orderRepository: OrderRepository,
    private val externalOrderCanceler: ExternalOrderCanceler,
) {
    fun cancelStoreOrder(device: StoreDevicePrincipal, orderKey: String): Order {
        device.validatePosDevice()
        val order = orderRepository.findByOrderKey(orderKey)
        if (order.storeId != device.storeId) {
            throw CoreException(ErrorType.FORBIDDEN, "해당 매장의 주문만 취소할 수 있습니다.")
        }

        return when (order.state) {
            OrderState.CANCELED -> throw CoreException(ErrorType.INVALID_REQUEST, "이미 취소된 주문입니다.")
            OrderState.CREATED -> {
                order.cancel()
                orderRepository.save(order)
            }
            OrderState.PAID -> {
                externalOrderCanceler.cancel(order)
                orderRepository.findByOrderKey(orderKey)
            }
        }
    }
}
