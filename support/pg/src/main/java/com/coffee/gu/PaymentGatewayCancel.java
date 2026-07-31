package com.coffee.gu;

public record PaymentGatewayCancel(
        String paymentKey,
        String cancelReason
) {
}
