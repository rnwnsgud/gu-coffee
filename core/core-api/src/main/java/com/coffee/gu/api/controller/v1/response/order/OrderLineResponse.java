package com.coffee.gu.api.controller.v1.response.order;

import java.math.BigDecimal;

public record OrderLineResponse(
        Long menuId,
        String menuName,
        String imageUrl,
        String description,
        Long quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}
