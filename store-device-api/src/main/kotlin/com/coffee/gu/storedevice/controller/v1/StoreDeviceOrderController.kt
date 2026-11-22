package com.coffee.gu.storedevice.controller.v1

import com.coffee.gu.response.ApiResponse
import com.coffee.gu.storedevice.auth.StoreDeviceAuthenticated
import com.coffee.gu.storedevice.auth.StoreDevicePrincipal
import com.coffee.gu.storedevice.controller.v1.request.CreateDeviceOrderRequest
import com.coffee.gu.storedevice.controller.v1.response.CreateDeviceOrderResponse
import com.coffee.gu.storedevice.order.StoreDeviceOrderService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class StoreDeviceOrderController(
    private val storeDeviceOrderService: StoreDeviceOrderService,
) {
    @PostMapping("/v1/devices/orders")
    fun createDeviceOrder(
        @StoreDeviceAuthenticated device: StoreDevicePrincipal,
        @RequestBody request: CreateDeviceOrderRequest,
    ): ApiResponse<CreateDeviceOrderResponse> {
        val orderKey = storeDeviceOrderService.createOrder(device, request.toNewOrderLines())
        return ApiResponse.success(CreateDeviceOrderResponse(orderKey))
    }
}
