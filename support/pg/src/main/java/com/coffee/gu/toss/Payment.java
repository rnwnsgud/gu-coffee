package com.coffee.gu.toss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Payment(
        String paymentKey,
        String orderId,
        TossPaymentsStatus status,
        BigDecimal totalAmount,
        String method,
        Card card,
        EasyPay easyPay,
        OffsetDateTime approvedAt,
        List<Cancel> cancels
) {
    public record Card(
            String approveNo
    ) {}
    public record EasyPay(
            String provider,
            BigDecimal amount,
            BigDecimal discountAmount
    ) {}
    public record Cancel(
            BigDecimal cancelAmount,
            String cancelReason,
            BigDecimal taxFreeAmount,
            BigDecimal taxExemptionAmount,
            BigDecimal refundableAmount,
            BigDecimal cardDiscountAmount,
            BigDecimal transferDiscountAmount,
            BigDecimal easyPayDiscountAmount,
            OffsetDateTime canceledAt,
            String transactionKey,
            String receiptKey,
            String cancelStatus
    ) {}
}
