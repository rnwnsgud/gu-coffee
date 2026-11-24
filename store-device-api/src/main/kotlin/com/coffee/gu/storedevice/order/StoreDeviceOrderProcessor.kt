package com.coffee.gu.storedevice.order

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.enums.OrderChannel
import com.coffee.gu.enums.StoreDeviceType
import com.coffee.gu.order.ExternalOrderCreator
import com.coffee.gu.order.NewOrder
import com.coffee.gu.order.NewOrderLine
import com.coffee.gu.storedevice.auth.StoreDevicePrincipal
import org.springframework.stereotype.Component

@Component
class StoreDeviceOrderProcessor(
    private val externalOrderCreator: ExternalOrderCreator,
) {
    fun create(device: StoreDevicePrincipal, lines: List<NewOrderLine>): String {
        val channel = when (device.deviceType) {
            StoreDeviceType.KIOSK -> OrderChannel.KIOSK
            StoreDeviceType.POS -> OrderChannel.POS
        }
        if (lines.isEmpty() || lines.any { it.menuId <= 0 || it.quantity <= 0 }) {
            throw CoreException(ErrorType.INVALID_REQUEST, "주문 항목이 비어 있거나 유효하지 않습니다.")
        }

        val newOrder = NewOrder(
            principal = Principal.guest(device.deviceCode),
            storeId = device.storeId,
            lines = lines,
            channel = channel,
        )
        return externalOrderCreator.create(newOrder)
    }
}
