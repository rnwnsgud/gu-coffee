package com.coffee.gu.payment;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.PaymentMethod;
import com.coffee.gu.enums.PaymentState;
import com.coffee.gu.order.Order;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
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
    private LocalDateTime createdAt;
    private int retryCount;

    public Payment(Long id, Principal principal, String orderKey, BigDecimal originalAmount, Long issuedCouponId, BigDecimal couponDiscount, BigDecimal amount, PaymentState state, String externalPaymentKey, PaymentMethod method, OffsetDateTime paidAt, String approveCode, LocalDateTime createdAt, int retryCount) {
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
        this.createdAt = createdAt;
        this.retryCount = retryCount;
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
                null, null, null, null,
                null, 0
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

    public Boolean isReady() {
        return state == PaymentState.READY;
    }

    public Boolean isFailed() {
        return state == PaymentState.FAILED;
    }

    public void fail() {
        this.state = PaymentState.FAILED;
    }

    public void increaseRetryCount() {
        this.retryCount++;
    }

    public boolean isRetryLimitExceeded(int maxLimit) {
        return this.retryCount >= maxLimit;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Boolean isExpired(Duration timeout) {
        if (createdAt == null) return false;
        return LocalDateTime.now().isAfter(createdAt.plus(timeout));
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
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
