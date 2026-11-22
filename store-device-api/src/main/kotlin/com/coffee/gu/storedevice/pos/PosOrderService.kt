package com.coffee.gu.storedevice.pos

import com.coffee.gu.enums.OrderState
import com.coffee.gu.order.Order
import com.coffee.gu.storedevice.auth.StoreDevicePrincipal
import org.springframework.stereotype.Service

@Service
class PosOrderService(
    private val posOrderReader: PosOrderReader,
    private val posOrderManager: PosOrderManager,
) {
    fun findStoreOrders(device: StoreDevicePrincipal, state: OrderState?): List<Order> {
        return posOrderReader.findStoreOrders(device, state)
    }

    fun cancelStoreOrder(device: StoreDevicePrincipal, orderKey: String): Order {
        return posOrderManager.cancelStoreOrder(device, orderKey)
    }
}
