package com.coffee.gu.api.controller.v1

import com.coffee.gu.Principal
import com.coffee.gu.api.controller.v1.request.CancelRequest
import com.coffee.gu.cancel.CancelService
import com.coffee.gu.enums.OrderState
import com.coffee.gu.order.OrderService
import com.coffee.gu.response.ApiResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class CancelController(
    private val cancelService: CancelService,
    private val orderService: OrderService,
) {
    @PostMapping("/v1/cancel")
    fun cancelOrder(
        principal: Principal,
        @RequestBody request: CancelRequest,
    ): ApiResponse<Unit> {
        val order = orderService.getOrder(request.orderKey, OrderState.PAID)
        order.validateOwner(principal)
        cancelService.cancel(order)
        return ApiResponse.success()
    }
}
