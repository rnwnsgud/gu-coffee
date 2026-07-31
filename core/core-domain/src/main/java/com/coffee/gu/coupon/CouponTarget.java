package com.coffee.gu.coupon;

import com.coffee.gu.enums.CouponTargetType;

public class CouponTarget {
    private Long id;
    private Long couponId;
    private CouponTargetType targetType;
    private Long targetId;

    public CouponTarget(Long id, Long couponId, CouponTargetType targetType, Long targetId) {
        this.id = id;
        this.couponId = couponId;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public Long getId() {
        return id;
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
