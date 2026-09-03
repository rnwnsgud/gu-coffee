package com.coffee.gu;

import com.coffee.gu.order.Order;
import com.coffee.gu.payment.Payment;
import com.coffee.gu.enums.TransactionType;
import com.coffee.gu.transaction.TransactionHistory;
import com.coffee.gu.transaction.TransactionHistoryRepository;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class TransactionHistoryManager {
    private final TransactionHistoryRepository transactionHistoryRepository;

    public TransactionHistoryManager(TransactionHistoryRepository transactionHistoryRepository) {
        this.transactionHistoryRepository = transactionHistoryRepository;
    }

    public void record(TransactionType type,
                       Order order,
                       Payment payment,
                       String message,
                       OffsetDateTime occurredAt) {
        transactionHistoryRepository.save(TransactionHistory.create(type, order, payment, message, occurredAt));
    }
}
