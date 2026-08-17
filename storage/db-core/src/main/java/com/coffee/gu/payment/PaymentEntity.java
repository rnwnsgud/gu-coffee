package com.coffee.gu.payment;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.PaymentMethod;
import com.coffee.gu.enums.PaymentState;
import com.coffee.gu.enums.PrincipalType;
import com.coffee.gu.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Table(
        name = "payment",
        indexes = {
                @Index(name = "udx_order_key", columnList = "orderKey", unique = true)
        }
)
@Entity
public class PaymentEntity extends BaseEntity {
    private String principalKey;
    @Enumerated(EnumType.STRING)
    private PrincipalType principalType;
    private String orderKey;
    private BigDecimal originalAmount;
    private Long issuedCouponId;
    private BigDecimal couponDiscount;
    private BigDecimal paidAmount;
    @Enumerated(EnumType.STRING)
    private PaymentState state;
    private String externalPaymentKey;
    @Enumerated(EnumType.STRING)
    private PaymentMethod method;
    private OffsetDateTime paidAt; // pg사, 카드사는 결제가 승인된 시점을 기준으로 수수료를 떼고 가맹점 계좌로 입금
    private String approveCode; // 카드사의 승인코드
    private int retryCount;

    public PaymentEntity() {}

    public PaymentEntity(String principalKey, PrincipalType principalType, String orderKey, BigDecimal originalAmount, Long issuedCouponId, BigDecimal couponDiscount, BigDecimal paidAmount, PaymentState state,
                         String externalPaymentKey, PaymentMethod method, OffsetDateTime paidAt, String approveCode, int retryCount) {
        this.principalKey = principalKey;
        this.principalType = principalType;
        this.orderKey = orderKey;
        this.originalAmount = originalAmount;
        this.issuedCouponId = issuedCouponId;
        this.couponDiscount = couponDiscount;
        this.paidAmount = paidAmount;
        this.state = state;
        this.externalPaymentKey = externalPaymentKey;
        this.method = method;
        this.paidAt = paidAt;
        this.approveCode = approveCode;
        this.retryCount = retryCount;
    }


    public static PaymentEntity from(Payment payment) {
        PaymentEntity entity = new PaymentEntity(
                payment.getPrincipal().getKey(),
                payment.getPrincipal().getType(),
                payment.getOrderKey(),
                payment.getOriginalAmount(),
                payment.getIssuedCouponId(),
                payment.getCouponDiscount(),
                payment.getAmount(),
                payment.getState(),
                payment.getExternalPaymentKey(),
                payment.getMethod(),
                payment.getPaidAt(),
                payment.getApproveCode(),
                payment.getRetryCount()
        );
        entity.id = payment.getId();
        return entity;
    }

    public Payment toModel() {
        return new Payment(
                this.id,
                new Principal(this.principalKey, this.principalType),
                this.orderKey,
                this.originalAmount,
                this.issuedCouponId,
                this.couponDiscount,
                this.paidAmount,
                this.state,
                this.externalPaymentKey,
                this.method,
                this.paidAt,
                this.approveCode,
                this.getCreatedAt(),
                this.retryCount
        );
    }

    public PaymentState getState() {
        return state;
    }

    public String getPrincipalKey() {
        return principalKey;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public Long getIssuedCouponId() {
        return issuedCouponId;
    }

    public PrincipalType getPrincipalType() {
        return principalType;
    }

    public String getOrderKey() {
        return orderKey;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public BigDecimal getCouponDiscount() {
        return couponDiscount;
    }

    public String getExternalPaymentKey() {
        return externalPaymentKey;
    }

    public int getRetryCount() {
        return retryCount;
    }
}
