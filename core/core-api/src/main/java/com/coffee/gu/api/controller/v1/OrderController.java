package com.coffee.gu.api.controller.v1;

import com.coffee.gu.Principal;
import com.coffee.gu.api.controller.v1.request.CreateOrderFromCartRequest;
import com.coffee.gu.api.controller.v1.request.CreateOrderRequest;
import com.coffee.gu.api.controller.v1.response.order.CreateOrderResponse;
import com.coffee.gu.api.controller.v1.response.order.OrderCheckoutResponse;
import com.coffee.gu.api.controller.v1.response.order.OrderListResponse;
import com.coffee.gu.api.controller.v1.response.order.OrderResponse;
import com.coffee.gu.cart.Cart;
import com.coffee.gu.cart.CartService;
import com.coffee.gu.coupon.IssuedCoupon;
import com.coffee.gu.coupon.IssuedCouponService;
import com.coffee.gu.order.Order;
import com.coffee.gu.order.OrderLine;
import com.coffee.gu.order.OrderService;
import com.coffee.gu.order.OrderSummary;
import com.coffee.gu.enums.OrderState;
import com.coffee.gu.response.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final IssuedCouponService issuedCouponService;

    public OrderController(OrderService orderService, CartService cartService, IssuedCouponService issuedCouponService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.issuedCouponService = issuedCouponService;
    }

    @PostMapping("/v1/orders")
    public ApiResponse<CreateOrderResponse> createOrder(Principal principal,
                                                        @RequestBody CreateOrderRequest request) {
        String orderKey = orderService.create(request.toNewOrder(principal));
        return ApiResponse.success(new CreateOrderResponse(orderKey));
    }

    @PostMapping("/v1/cart-orders")
    public ApiResponse<CreateOrderResponse> createOrderFromCart(
            Principal principal,
            @RequestBody CreateOrderFromCartRequest request) {
        Cart cart = cartService.getCart(principal);
        String orderKey = orderService.create(cart.toNewOrder(request.cartItemIds(), request.storeId()));
        return ApiResponse.success(new CreateOrderResponse(orderKey));
    }

    @GetMapping("/v1/orders/{orderKey}/checkout")
    public ApiResponse<OrderCheckoutResponse> getOrderForCheckout(Principal principal, @PathVariable String orderKey) {
        Order order = orderService.getOrder(orderKey, OrderState.CREATED);
        List<IssuedCoupon> issuedCoupons = issuedCouponService.getIssuedCouponsForCheckout(principal, order);
        return ApiResponse.success(OrderCheckoutResponse.of(order, issuedCoupons));
    }

    @GetMapping("/v1/orders")
    public ApiResponse<List<OrderListResponse>> getOrders(Principal principal) {
        List<OrderSummary> orders = orderService.getPaidOrders(principal);
    return ApiResponse.success(OrderListResponse.from(orders));
    }

    @GetMapping("/v1/orders/{orderKey}")
    public ApiResponse<OrderResponse> getOrder(Principal principal,
                                               @PathVariable String orderKey) {
        Order order = orderService.getOrder(orderKey, OrderState.PAID);
        return ApiResponse.success(OrderResponse.from(order));
    }
}
