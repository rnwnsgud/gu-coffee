package com.coffee.gu.api.controller.v1.request

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.enums.OrderChannel
import com.coffee.gu.order.NewOrder
import com.coffee.gu.order.NewOrderLine

data class CreateExternalOrderLineRequest(
    val menuId: Long,
    val quantity: Long,
)

data class CreateExternalOrderRequest(
    val storeId: Long,
    val channel: OrderChannel,
    val externalCustomerKey: String,
    val lines: List<CreateExternalOrderLineRequest>,
) {
    fun toNewOrder(): NewOrder {
        if (channel != OrderChannel.KIOSK) {
            throw CoreException(
                ErrorType.INVALID_REQUEST,
                "외부 주문 API는 KIOSK 채널만 지원합니다. 배달 플랫폼 주문은 웹훅 API를 사용해 주세요.",
            )
        }
        if (lines.isEmpty() || lines.any { it.quantity <= 0 }) {
            throw CoreException(ErrorType.INVALID_REQUEST, null)
        }
        return NewOrder(
            principal = Principal.guest(externalCustomerKey),
            storeId = storeId,
            lines = lines.map { NewOrderLine(menuId = it.menuId, quantity = it.quantity) },
            channel = channel,
        )
    }
}
