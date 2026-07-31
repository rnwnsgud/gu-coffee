package com.coffee.gu.api.controller.v1.response.order;

import com.coffee.gu.api.controller.v1.response.IssuedCouponResponse;
import com.coffee.gu.coupon.IssuedCoupon;
import com.coffee.gu.order.Order;

import java.math.BigDecimal;
import java.util.List;

public record OrderCheckoutResponse(
        String key,
        String name,
        BigDecimal totalPrice,
        List<OrderLineResponse> lines,
        List<IssuedCouponResponse> usableCoupons
) {
    public static OrderCheckoutResponse of(Order order, List<IssuedCoupon> issuedCoupons) {
        return new OrderCheckoutResponse(
                order.getKey(),
                order.getName(),
                order.getTotalPrice(),
                order.getLines()
                        .stream()
                        .map(line -> new OrderLineResponse(
                                line.getMenuId(),
                                line.getMenuName(),
                                line.getImageUrl(),
                                line.getDescription(),
                                line.getQuantity(),
                                line.getUnitPrice(),
                                line.getTotalPrice()
                        )).toList(),
                issuedCoupons.stream().map(IssuedCouponResponse::from).toList()
        );
    }
}
