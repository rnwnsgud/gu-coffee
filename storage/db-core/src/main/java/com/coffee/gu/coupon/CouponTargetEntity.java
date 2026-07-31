package com.coffee.gu.coupon;

import com.coffee.gu.enums.CouponTargetType;
import com.coffee.gu.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Table(name = "coupon_target")
@Entity
public class CouponTargetEntity extends BaseEntity {
    private Long couponId;
    @Enumerated(EnumType.STRING)
    private CouponTargetType targetType;
    private Long targetId;

    protected CouponTargetEntity() {}

    public CouponTargetEntity(Long couponId, CouponTargetType targetType, Long targetId) {
        this.couponId = couponId;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public CouponTarget toModel() {
        return new CouponTarget(id, couponId, targetType, targetId);
    }

    public Long getCouponId() {
        return couponId;
    }

    public CouponTargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }
}
