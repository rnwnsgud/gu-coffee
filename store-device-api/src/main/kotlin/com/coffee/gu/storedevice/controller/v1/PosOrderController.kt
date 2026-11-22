package com.coffee.gu.storedevice.controller.v1

import com.coffee.gu.enums.OrderState
import com.coffee.gu.response.ApiResponse
import com.coffee.gu.storedevice.auth.StoreDeviceAuthenticated
import com.coffee.gu.storedevice.auth.StoreDevicePrincipal
import com.coffee.gu.storedevice.controller.v1.response.CancelPosOrderResponse
import com.coffee.gu.storedevice.controller.v1.response.PosOrderSummaryResponse
import com.coffee.gu.storedevice.pos.PosOrderService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PosOrderController(
    private val posOrderService: PosOrderService,
) {
    @GetMapping("/v1/devices/pos/orders")
    fun findStoreOrders(
        @StoreDeviceAuthenticated device: StoreDevicePrincipal,
        @RequestParam(required = false) state: OrderState?,
    ): ApiResponse<List<PosOrderSummaryResponse>> {
        val orders = posOrderService.findStoreOrders(device, state)
        return ApiResponse.success(orders.map { PosOrderSummaryResponse.from(it) })
    }

    @PostMapping("/v1/devices/pos/orders/{orderKey}/cancel")
    fun cancelStoreOrder(
        @StoreDeviceAuthenticated device: StoreDevicePrincipal,
        @PathVariable orderKey: String,
    ): ApiResponse<CancelPosOrderResponse> {
        val canceledOrder = posOrderService.cancelStoreOrder(device, orderKey)
        return ApiResponse.success(CancelPosOrderResponse.from(canceledOrder))
    }
}
