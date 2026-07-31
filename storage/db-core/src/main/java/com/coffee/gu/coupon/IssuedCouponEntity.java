package com.coffee.gu.coupon;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.IssuedCouponState;
import com.coffee.gu.BaseEntity;
import com.coffee.gu.enums.PrincipalType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Table(name = "issued_coupon")
@Entity
public class IssuedCouponEntity extends BaseEntity {

    private String principalKey;
    @Enumerated(EnumType.STRING)
    private PrincipalType principalType;
    private Long couponId;
    private IssuedCouponState state;

    protected IssuedCouponEntity() {}

    public IssuedCouponEntity(String principalKey, PrincipalType principalType, Long couponId, IssuedCouponState state) {
        this.principalKey = principalKey;
        this.principalType = principalType;
        this.couponId = couponId;
        this.state = state;
    }

    public static IssuedCouponEntity from(IssuedCoupon issuedCoupon) {
        return new IssuedCouponEntity(
                issuedCoupon.getPrincipal().getKey(),
                issuedCoupon.getPrincipal().getType(),
                issuedCoupon.getCoupon().getId(),
                issuedCoupon.getState()
        );
    }

    public IssuedCoupon toModel(Coupon coupon) {
        return new IssuedCoupon(
                this.id,
                new Principal(this.principalKey, this.principalType),
                this.state,
                coupon
        );
    }

    public String getPrincipalKey() {
        return principalKey;
    }

    public PrincipalType getPrincipalType() {
        return principalType;
    }

    public Long getCouponId() {
        return couponId;
    }

    public IssuedCouponState getState() {
        return state;
    }
}
