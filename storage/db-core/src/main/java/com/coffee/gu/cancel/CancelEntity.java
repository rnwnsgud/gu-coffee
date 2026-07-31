package com.coffee.gu.cancel;


import com.coffee.gu.Principal;
import com.coffee.gu.enums.PrincipalType;
import com.coffee.gu.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cancel")
public class CancelEntity extends BaseEntity {
    private String principalKey;
    @Enumerated(EnumType.STRING)
    private PrincipalType principalType;
    private String orderKey;
    private Long paymentId;
    private BigDecimal originalAmount;
    private Long issuedCouponId;
    private BigDecimal couponDiscount;
    private BigDecimal paidAmount;
    private BigDecimal canceledAmount;
    private String externalCancelKey;
    private OffsetDateTime canceledAt;

    public CancelEntity() {}

    public CancelEntity(String principalKey, PrincipalType principalType, String orderKey, Long paymentId, BigDecimal originalAmount, Long issuedCouponId, BigDecimal couponDiscount, BigDecimal paidAmount, BigDecimal canceledAmount, String externalCancelKey, OffsetDateTime canceledAt) {
        this.principalKey = principalKey;
        this.principalType = principalType;
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

    public static CancelEntity from(Cancel cancel) {
        return new CancelEntity(
                cancel.getPrincipal().getKey(),
                cancel.getPrincipal().getType(),
                cancel.getOrderKey(),
                cancel.getPaymentId(),
                cancel.getOriginalAmount(),
                cancel.getIssuedCouponId(),
                cancel.getCouponDiscount(),
                cancel.getPaidAmount(),
                cancel.getCanceledAmount(),
                cancel.getExternalCancelKey(),
                cancel.getCanceledAt()
        );
    }

    public Cancel toModel() {
        return new Cancel(
                this.id,
                new Principal(this.principalKey, this.principalType),
                this.orderKey,
                this.paymentId,
                this.originalAmount,
                this.issuedCouponId,
                this.couponDiscount,
                this.paidAmount,
                this.canceledAmount,
                this.externalCancelKey,
                this.canceledAt
        );
    }

    public OffsetDateTime getCanceledAt() {
        return canceledAt;
    }
}
