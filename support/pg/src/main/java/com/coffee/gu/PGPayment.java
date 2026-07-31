package com.coffee.gu;

import java.math.BigDecimal;

public record PGPayment(
        String paymentKey,
        String orderKey,
        BigDecimal amount,
        PaymentGatewayStatus status
) {
}
