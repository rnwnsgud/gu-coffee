package com.coffee.gu.api.controller.v1

import com.coffee.gu.api.controller.v1.request.CreateExternalOrderRequest
import com.coffee.gu.api.controller.v1.response.order.CreateOrderResponse
import com.coffee.gu.order.OrderService
import com.coffee.gu.response.ApiResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ExternalOrderController(
    private val orderService: OrderService,
) {
    @PostMapping("/v1/external-orders")
    fun createExternalOrder(
        @RequestBody request: CreateExternalOrderRequest,
    ): ApiResponse<CreateOrderResponse> {
        val orderKey = orderService.create(request.toNewOrder())
        return ApiResponse.success(CreateOrderResponse(orderKey))
    }
}
