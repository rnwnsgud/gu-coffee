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

    public static PaymentApprovalResult alreadyApproved(String orderKey, String paymentKey, OffsetDateTime approvedAt) {
        return new PaymentApprovalResult(
                orderKey,
                paymentKey,
                PaymentState.SUCCESS,
                OrderState.PAID,
                true,
                approvedAt
        );
    }

    public static PaymentApprovalResult fromExisting(Payment payment) {
        OrderState orderState = (payment.getState() == PaymentState.SUCCESS) ? OrderState.PAID : OrderState.CREATED;
        return new PaymentApprovalResult(
                payment.getOrderKey(),
                payment.getExternalPaymentKey(),
                payment.getState(),
                orderState,
                true,
                payment.getPaidAt()
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
