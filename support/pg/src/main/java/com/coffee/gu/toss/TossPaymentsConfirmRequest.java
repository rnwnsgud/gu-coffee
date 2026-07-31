package com.coffee.gu.toss;


import java.math.BigDecimal;

public record TossPaymentsConfirmRequest(
        String paymentKey,
        String orderId,
        BigDecimal amount
) {

}
