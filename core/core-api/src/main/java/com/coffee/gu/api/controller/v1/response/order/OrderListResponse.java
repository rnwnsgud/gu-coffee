package com.coffee.gu.api.controller.v1.response.order;

import com.coffee.gu.order.OrderSummary;
import com.coffee.gu.enums.OrderState;

import java.math.BigDecimal;
import java.util.List;

public record OrderListResponse(
        String key,
        String name,
        BigDecimal totalPrice,
        OrderState state
) {
    public static OrderListResponse from(OrderSummary order) {
        return new OrderListResponse(
          order.key(),
          order.name(),
          order.totalPrice(),
          order.state()
        );
    }

    public static List<OrderListResponse> from(List<OrderSummary> orders) {
        return orders.stream()
                .map(OrderListResponse::from)
                .toList();
    }
}
