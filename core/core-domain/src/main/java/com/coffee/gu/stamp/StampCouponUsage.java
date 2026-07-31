package com.coffee.gu.stamp;

import java.time.LocalDateTime;

public class StampCouponUsage {
    private Long id;
    private Long stampId;
    private Long issuedCouponId;
    private LocalDateTime usedAt;

    public StampCouponUsage(Long id, Long stampId, Long issuedCouponId, LocalDateTime usedAt) {
        this.id = id;
        this.stampId = stampId;
        this.issuedCouponId = issuedCouponId;
        this.usedAt = usedAt;
    }

    public static StampCouponUsage create(Long stampId, Long issuedCouponId, LocalDateTime now) {
        return new StampCouponUsage(null, stampId, issuedCouponId, now);
    }

    public Long getId() {
        return id;
    }

    public Long getStampId() {
        return stampId;
    }

    public Long getIssuedCouponId() {
        return issuedCouponId;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }
}
