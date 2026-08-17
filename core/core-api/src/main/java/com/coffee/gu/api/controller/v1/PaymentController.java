package com.coffee.gu.api.controller.v1;

import com.coffee.gu.Principal;
import com.coffee.gu.api.controller.v1.request.CreatePaymentRequest;
import com.coffee.gu.api.controller.v1.response.payment.CreatePaymentResponse;
import com.coffee.gu.api.controller.v1.response.payment.PaymentResponse;
import com.coffee.gu.auth.Authenticated;
import com.coffee.gu.coupon.IssuedCoupon;
import com.coffee.gu.coupon.IssuedCouponService;
import com.coffee.gu.order.Order;
import com.coffee.gu.order.OrderService;
import com.coffee.gu.payment.PaymentApprovalResult;
import com.coffee.gu.payment.PaymentService;
import com.coffee.gu.enums.OrderState;
import com.coffee.gu.response.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PaymentController {
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final IssuedCouponService issuedCouponService;

    public PaymentController(PaymentService paymentService, OrderService orderService, IssuedCouponService issuedCouponService) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.issuedCouponService = issuedCouponService;
    }

    @PostMapping("/v1/payments")
    public ApiResponse<CreatePaymentResponse> createPayment(@Authenticated Principal principal, @RequestBody CreatePaymentRequest request) {
        Order order = orderService.getOrder(request.orderKey(), OrderState.CREATED);
        order.validateOwner(principal);
        List<IssuedCoupon> issuedCoupons = issuedCouponService.getIssuedCouponsForCheckout(principal, order);
        Long paymentId = paymentService.createPayment(order, request.toPaymentDiscount(issuedCoupons, order.getTotalPrice()));
        return ApiResponse.success(new CreatePaymentResponse(paymentId));
    }

    @PostMapping("/v1/payments/confirm")
    public ApiResponse<PaymentResponse> confirm(
            @Authenticated Principal principal,
            @RequestParam("orderId") String orderId
    ) {
        Order order = orderService.getOrder(orderId);
        order.validateOwner(principal);
        PaymentApprovalResult result = paymentService.approvePayment(order);
        return ApiResponse.success(new PaymentResponse(result.getPaymentState()));
    }

    @PostMapping("/v1/payments/fail")
    public ApiResponse<?> fail(
            @Authenticated Principal principal,
            @RequestParam("orderId") String orderId,
            @RequestParam("code") String code,
            @RequestParam("message") String message
    ) {
        Order order = orderService.getOrder(orderId);
        order.validateOwner(principal);
        paymentService.fail(order, code, message);
        return ApiResponse.success();
    }
}
