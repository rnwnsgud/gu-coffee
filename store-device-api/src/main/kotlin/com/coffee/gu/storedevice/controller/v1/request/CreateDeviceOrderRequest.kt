package com.coffee.gu.storedevice.controller.v1.request

import com.coffee.gu.order.NewOrderLine

data class CreateDeviceOrderLineRequest(
    val menuId: Long,
    val quantity: Long,
)

data class CreateDeviceOrderRequest(
    val lines: List<CreateDeviceOrderLineRequest>,
) {
    fun toNewOrderLines(): List<NewOrderLine> {
        return lines.map { NewOrderLine(menuId = it.menuId, quantity = it.quantity) }
    }
}
