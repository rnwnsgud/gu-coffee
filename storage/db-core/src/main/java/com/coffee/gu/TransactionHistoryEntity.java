package com.coffee.gu;

import com.coffee.gu.enums.PrincipalType;
import com.coffee.gu.enums.TransactionType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transaction_history")
public class TransactionHistoryEntity extends BaseEntity{
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    private String principalKey;
    @Enumerated(EnumType.STRING)
    private PrincipalType principalType;
    private String orderKey;
    private Long paymentId;
    private String externalTransactionKey;
    private BigDecimal amount;
    private String message;
    private OffsetDateTime occurredAt;

    protected TransactionHistoryEntity() {}

    public TransactionHistoryEntity(TransactionType type, String principalKey, PrincipalType principalType, String orderKey, Long paymentId, String externalTransactionKey, BigDecimal amount, String message, OffsetDateTime occurredAt) {
        this.type = type;
        this.principalKey = principalKey;
        this.principalType = principalType;
        this.orderKey = orderKey;
        this.paymentId = paymentId;
        this.externalTransactionKey = externalTransactionKey;
        this.amount = amount;
        this.message = message;
        this.occurredAt = occurredAt;
    }

    public static TransactionHistoryEntity from(TransactionHistory transactionHistory) {
        return new TransactionHistoryEntity(
                transactionHistory.getType(),
                transactionHistory.getPrincipal().getKey(),
                transactionHistory.getPrincipal().getType(),
                transactionHistory.getOrderKey(),
                transactionHistory.getPaymentId(),
                transactionHistory.getExternalTransactionKey(),
                transactionHistory.getAmount(),
                transactionHistory.getMessage(),
                transactionHistory.getOccurredAt()
        );
    }

    public TransactionHistory toModel() {
        return new TransactionHistory(
                id,
                type,
                new Principal(principalKey, principalType),
                orderKey,
                paymentId,
                externalTransactionKey,
                amount,
                message,
                occurredAt
        );
    }

}
