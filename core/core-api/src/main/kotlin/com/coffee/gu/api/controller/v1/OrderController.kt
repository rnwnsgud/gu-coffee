package com.coffee.gu.api.controller.v1

import com.coffee.gu.Principal
import com.coffee.gu.api.controller.v1.request.CreateOrderFromCartRequest
import com.coffee.gu.api.controller.v1.request.CreateOrderRequest
import com.coffee.gu.api.controller.v1.response.order.CreateOrderResponse
import com.coffee.gu.api.controller.v1.response.order.OrderCheckoutResponse
import com.coffee.gu.api.controller.v1.response.order.OrderListResponse
import com.coffee.gu.api.controller.v1.response.order.OrderResponse
import com.coffee.gu.auth.Authenticated
import com.coffee.gu.cart.CartService
import com.coffee.gu.coupon.IssuedCouponService
import com.coffee.gu.enums.OrderState
import com.coffee.gu.order.OrderService
import com.coffee.gu.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController(
    private val orderService: OrderService,
    private val cartService: CartService,
    private val issuedCouponService: IssuedCouponService,
) {
    @PostMapping("/v1/orders")
    fun createOrder(
        @Authenticated principal: Principal,
        @RequestBody request: CreateOrderRequest,
    ): ApiResponse<CreateOrderResponse> {
        val orderKey = orderService.create(request.toNewOrder(principal))
        return ApiResponse.success(CreateOrderResponse(orderKey))
    }

    @PostMapping("/v1/cart-orders")
    fun createOrderFromCart(
        @Authenticated principal: Principal,
        @RequestBody request: CreateOrderFromCartRequest,
    ): ApiResponse<CreateOrderResponse> {
        val cart = cartService.getCart(principal)
        val orderKey = orderService.create(cart.toNewOrder(request.cartItemIds, request.storeId))
        return ApiResponse.success(CreateOrderResponse(orderKey))
    }

    @GetMapping("/v1/orders/{orderKey}/checkout")
    fun getOrderForCheckout(
        @Authenticated principal: Principal,
        @PathVariable orderKey: String,
    ): ApiResponse<OrderCheckoutResponse> {
        val order = orderService.getOrder(orderKey, OrderState.CREATED)
        order.validateOwner(principal)
        val issuedCoupons = issuedCouponService.getIssuedCouponsForCheckout(principal, order)
        return ApiResponse.success(OrderCheckoutResponse.of(order, issuedCoupons))
    }

    @GetMapping("/v1/orders")
    fun getOrders(@Authenticated principal: Principal): ApiResponse<List<OrderListResponse>> {
        val orders = orderService.getPaidOrders(principal)
        return ApiResponse.success(OrderListResponse.from(orders))
    }

    @GetMapping("/v1/orders/{orderKey}")
    fun getOrder(
        @Authenticated principal: Principal,
        @PathVariable orderKey: String,
    ): ApiResponse<OrderResponse> {
        val order = orderService.getOrder(orderKey, OrderState.PAID)
        order.validateOwner(principal)
        return ApiResponse.success(OrderResponse.from(order))
    }
}
