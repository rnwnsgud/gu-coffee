package com.coffee.gu.payment;

import com.coffee.gu.enums.OrderState;
import com.coffee.gu.enums.PaymentState;

import java.time.OffsetDateTime;

public class PaymentApprovalResult {
    private String orderKey;
    private String paymentKey;
    private PaymentState paymentState;
    private OrderState orderState;
    private Boolean idempotent;
    private OffsetDateTime approvedAt;

    public PaymentApprovalResult(String orderKey, String paymentKey, PaymentState paymentState, OrderState orderState, Boolean idempotent, OffsetDateTime approvedAt) {
        this.orderKey = orderKey;
        this.paymentKey = paymentKey;
        this.paymentState = paymentState;
        this.orderState = orderState;
        this.idempotent = idempotent;
        this.approvedAt = approvedAt;
    }

    public static PaymentApprovalResult approved(String orderKey, String paymentKey, OffsetDateTime approvedAt) {
        return new PaymentApprovalResult(
                orderKey,
                paymentKey,
                PaymentState.SUCCESS,
                OrderState.PAID,
                false,
                approvedAt
        );
    }

    public static PaymentApprovalResult alReadyApproved(String orderKey, String paymentKey, OffsetDateTime approvedAt) {
        return new PaymentApprovalResult(
                orderKey,
                paymentKey,
                PaymentState.SUCCESS,
                OrderState.PAID,
                true,
                approvedAt
        );
    }

    public static PaymentApprovalResult failed(String orderKey, String paymentKey, OffsetDateTime approvedAt) {
        return new PaymentApprovalResult(
                orderKey,
                paymentKey,
                PaymentState.FAILED,
                OrderState.PAID,
                false,
                approvedAt
        );
    }

    public PaymentState getPaymentState() {
        return paymentState;
    }
}
