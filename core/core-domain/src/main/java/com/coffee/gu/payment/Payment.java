package com.coffee.gu.payment;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.PaymentMethod;
import com.coffee.gu.enums.PaymentState;
import com.coffee.gu.order.Order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class Payment {
    private Long id;
    private Principal principal;
    private String orderKey;
    private BigDecimal originalAmount;
    private Long issuedCouponId;
    private BigDecimal couponDiscount;
    private BigDecimal amount;
    private PaymentState state;
    private String externalPaymentKey;
    private PaymentMethod method;
    private OffsetDateTime paidAt;
    private String approveCode;

    public Payment(Long id, Principal principal, String orderKey, BigDecimal originalAmount, Long issuedCouponId, BigDecimal couponDiscount, BigDecimal amount, PaymentState state, String externalPaymentKey, PaymentMethod method, OffsetDateTime paidAt, String approveCode) {
        this.id = id;
        this.principal = principal;
        this.orderKey = orderKey;
        this.originalAmount = originalAmount;
        this.issuedCouponId = issuedCouponId;
        this.couponDiscount = couponDiscount;
        this.amount = amount;
        this.state = state;
        this.externalPaymentKey = externalPaymentKey;
        this.method = method;
        this.paidAt = paidAt;
        this.approveCode = approveCode;
    }

    public static Payment create(Order order, PaymentDiscount paymentDiscount) {
        return new Payment(
                null,
                order.getPrincipal(),
                order.getKey(),
                order.getTotalPrice(),
                paymentDiscount.useIssuedCouponId(),
                paymentDiscount.couponDiscount(),
                paymentDiscount.getPaidAmount(),
                PaymentState.READY,
                null, null, null, null
        );
    }

    public void success(String externalPaymentKey, PaymentMethod method, String approveCode, OffsetDateTime paidAt) {
        this.state = PaymentState.SUCCESS;
        this.externalPaymentKey = externalPaymentKey;
        this.method = method;
        this.approveCode = approveCode;
        this.paidAt = paidAt;
    }

    public void prepare() {
        this.state = PaymentState.PENDING_PG;
    }

    public Boolean isPaid() {
        return state == PaymentState.SUCCESS;
    }

    public void fail() {
        this.state = PaymentState.FAILED;
    }

    public Boolean hasAppliedCoupon() {
        return issuedCouponId != null && issuedCouponId > 0;
    }

    public Long getId() {
        return id;
    }

    public String getOrderKey() {
        return orderKey;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public Long getIssuedCouponId() {
        return issuedCouponId;
    }

    public BigDecimal getCouponDiscount() {
        return couponDiscount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getExternalPaymentKey() {
        return externalPaymentKey;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public String getApproveCode() {
        return approveCode;
    }

    public PaymentState getState() {
        return state;
    }


    public Principal getPrincipal() {
        return principal;
    }
}
