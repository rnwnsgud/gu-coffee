package com.coffee.gu.api.controller.v1

import com.coffee.gu.Principal
import com.coffee.gu.api.controller.v1.request.CreatePaymentRequest
import com.coffee.gu.api.controller.v1.response.payment.CreatePaymentResponse
import com.coffee.gu.api.controller.v1.response.payment.PaymentResponse
import com.coffee.gu.auth.Authenticated
import com.coffee.gu.coupon.IssuedCouponService
import com.coffee.gu.enums.OrderState
import com.coffee.gu.order.OrderService
import com.coffee.gu.payment.PaymentService
import com.coffee.gu.response.ApiResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PaymentController(
    private val paymentService: PaymentService,
    private val orderService: OrderService,
    private val issuedCouponService: IssuedCouponService,
) {
    @PostMapping("/v1/payments")
    fun createPayment(
        @Authenticated principal: Principal,
        @RequestBody request: CreatePaymentRequest,
    ): ApiResponse<CreatePaymentResponse> {
        val order = orderService.getOrder(request.orderKey, OrderState.CREATED)
        order.validateOwner(principal)
        val issuedCoupons = issuedCouponService.getIssuedCouponsForCheckout(principal, order)
        val paymentId = paymentService.createPayment(order, request.toPaymentDiscount(issuedCoupons, order.totalPrice))
        return ApiResponse.success(CreatePaymentResponse(paymentId))
    }

    @PostMapping("/v1/payments/confirm")
    fun confirm(
        @Authenticated principal: Principal,
        @RequestParam("orderId") orderId: String,
    ): ApiResponse<PaymentResponse> {
        val order = orderService.getOrder(orderId)
        order.validateOwner(principal)
        val result = paymentService.approvePayment(order)
        return ApiResponse.success(PaymentResponse(result.paymentState))
    }

    @PostMapping("/v1/payments/fail")
    fun fail(
        @Authenticated principal: Principal,
        @RequestParam("orderId") orderId: String,
        @RequestParam("code") code: String,
        @RequestParam("message") message: String,
    ): ApiResponse<Unit> {
        val order = orderService.getOrder(orderId)
        order.validateOwner(principal)
        paymentService.fail(order, code, message)
        return ApiResponse.success()
    }
}
