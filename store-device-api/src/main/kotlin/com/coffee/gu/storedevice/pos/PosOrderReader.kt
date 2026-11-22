package com.coffee.gu.storedevice.pos

import com.coffee.gu.enums.OrderState
import com.coffee.gu.order.Order
import com.coffee.gu.order.OrderRepository
import com.coffee.gu.storedevice.auth.StoreDevicePrincipal
import org.springframework.stereotype.Component

@Component
class PosOrderReader(
    private val orderRepository: OrderRepository,
) {
    fun findStoreOrders(device: StoreDevicePrincipal, state: OrderState?): List<Order> {
        device.validatePosDevice()
        return orderRepository.findAllByStoreId(device.storeId, state)
    }
}
