package com.coffee.gu.storedevice.order

import com.coffee.gu.order.NewOrderLine
import com.coffee.gu.storedevice.auth.StoreDevicePrincipal
import org.springframework.stereotype.Service

@Service
class StoreDeviceOrderService(
    private val storeDeviceOrderProcessor: StoreDeviceOrderProcessor,
) {
    fun createOrder(device: StoreDevicePrincipal, lines: List<NewOrderLine>): String {
        return storeDeviceOrderProcessor.create(device, lines)
    }
}
