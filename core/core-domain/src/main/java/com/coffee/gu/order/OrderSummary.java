package com.coffee.gu.order;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.OrderState;

import java.math.BigDecimal;

public record OrderSummary(
        String key,
        String name,
        Principal principal,
        BigDecimal totalPrice,
        OrderState state
) {
}
