package com.coffee.gu.admin.domain;

import java.math.BigDecimal;

public record AdminPrice(
        BigDecimal costPrice,
        BigDecimal salesPrice
) {
    public AdminPrice {
        if (salesPrice.compareTo(costPrice) < 0) {
            throw new IllegalArgumentException("Sales price cannot be greater than cost price.");
        }
    }

}
