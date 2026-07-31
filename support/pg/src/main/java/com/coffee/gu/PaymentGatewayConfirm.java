package com.coffee.gu;

import java.math.BigDecimal;

public record PaymentGatewayConfirm(
        String paymentKey,
        String orderKey,
        BigDecimal amount
) {
}

