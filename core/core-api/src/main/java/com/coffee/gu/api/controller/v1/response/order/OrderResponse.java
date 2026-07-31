package com.coffee.gu.api.controller.v1.response.order;

import com.coffee.gu.order.Order;
import com.coffee.gu.enums.OrderState;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
        String key,
        String name,
        BigDecimal totalPrice,
        OrderState state,
        List<OrderLineResponse> lines
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getKey(),
                order.getName(),
                order.getTotalPrice(),
                order.getState(),
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
                        ))
                        .toList()
        );
    }
}
