package com.coffee.gu.cancel;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.PrincipalType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class Cancel {
    private Long id;
    private Principal principal;
    private String orderKey;
    private Long paymentId;
    private BigDecimal originalAmount;
    private Long issuedCouponId;
    private BigDecimal couponDiscount;
    private BigDecimal paidAmount;
    private BigDecimal canceledAmount;
    private String externalCancelKey;
    private OffsetDateTime canceledAt;

    public Cancel(Long id, Principal principal, String orderKey, Long paymentId, BigDecimal originalAmount, Long issuedCouponId, BigDecimal couponDiscount, BigDecimal paidAmount, BigDecimal canceledAmount, String externalCancelKey, OffsetDateTime canceledAt) {
        this.id = id;
        this.principal = principal;
        this.orderKey = orderKey;
        this.paymentId = paymentId;
        this.originalAmount = originalAmount;
        this.issuedCouponId = issuedCouponId;
        this.couponDiscount = couponDiscount;
        this.paidAmount = paidAmount;
        this.canceledAmount = canceledAmount;
        this.externalCancelKey = externalCancelKey;
        this.canceledAt = canceledAt;
    }

    public Long getId() {
        return id;
    }

    public String getOrderKey() {
        return orderKey;
    }

    public Long getPaymentId() {
        return paymentId;
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

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public BigDecimal getCanceledAmount() {
        return canceledAmount;
    }

    public String getExternalCancelKey() {
        return externalCancelKey;
    }

    public OffsetDateTime getCanceledAt() {
        return canceledAt;
    }

    public Principal getPrincipal() {
        return principal;
    }
}
