package com.coffee.gu;

import com.coffee.gu.enums.TransactionType;
import com.coffee.gu.order.Order;
import com.coffee.gu.payment.Payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TransactionHistory {
    private Long id;
    private TransactionType type;
    private Principal principal;
    private String orderKey;
    private Long paymentId;
    private String externalTransactionKey;
    private BigDecimal amount;
    private String message;
    private OffsetDateTime occurredAt;

    public TransactionHistory(Long id, TransactionType type, Principal principal, String orderKey, Long paymentId, String externalTransactionKey, BigDecimal amount, String message, OffsetDateTime occurredAt) {
        this.id = id;
        this.type = type;
        this.principal = principal;
        this.orderKey = orderKey;
        this.paymentId = paymentId;
        this.externalTransactionKey = externalTransactionKey;
        this.amount = amount;
        this.message = message;
        this.occurredAt = occurredAt;
    }

    public static TransactionHistory create(TransactionType type, Order order, Payment payment, String message, OffsetDateTime occurredAt) {
        return new TransactionHistory(
                null,
                type,
                order.getPrincipal(),
                order.getKey(),
                payment.getId(),
                payment.getExternalPaymentKey(),
                payment.getAmount(),
                message,
                occurredAt
        );
    }

    public Long getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public String getOrderKey() {
        return orderKey;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public String getExternalTransactionKey() {
        return externalTransactionKey;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getMessage() {
        return message;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }
}
