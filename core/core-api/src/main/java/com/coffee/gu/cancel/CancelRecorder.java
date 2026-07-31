package com.coffee.gu.cancel;

import com.coffee.gu.TransactionHistoryManager;
import com.coffee.gu.enums.TransactionType;
import com.coffee.gu.order.Order;
import com.coffee.gu.payment.Payment;
import org.springframework.stereotype.Component;

@Component
public class CancelRecorder {

    private final CancelManager cancelManager;
    private final TransactionHistoryManager transactionHistoryManager;

    public CancelRecorder(CancelManager cancelManager, TransactionHistoryManager transactionHistoryManager) {
        this.cancelManager = cancelManager;
        this.transactionHistoryManager = transactionHistoryManager;
    }

    public Cancel record(Payment payment, Order order) {
        Cancel cancel = cancelManager.cancel(payment, "PG_API_응답_취소_고유_값_저장");
        transactionHistoryManager.record(TransactionType.CANCEL, order, payment, "Cancel processed", cancel.getCanceledAt());
        return cancel;
    }
}
