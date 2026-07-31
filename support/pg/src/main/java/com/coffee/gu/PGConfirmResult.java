package com.coffee.gu;

import com.coffee.gu.enums.PaymentMethod;

import java.time.OffsetDateTime;

public record PGConfirmResult(
        String orderId,
        String paymentKey,
        boolean isConfirmed,
        PaymentMethod paymentMethod,
        String approveCode,
        OffsetDateTime approvedAt
) {
    public static PGConfirmResult success(
            String orderId,
            String paymentKey,
            PaymentMethod paymentMethod,
            String approveCode,
            OffsetDateTime approvedAt
    ) {
        return new PGConfirmResult(orderId, paymentKey, true, paymentMethod, approveCode, approvedAt);
    }

    public static PGConfirmResult fail(
            String orderId,
            String paymentKey
    ) {
        return new PGConfirmResult(orderId, paymentKey, false, null, null, null);
    }
}
