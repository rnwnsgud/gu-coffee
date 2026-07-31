package com.coffee.gu.stamp;

import com.coffee.gu.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Table(name = "stamp_coupon_usage")
@Entity
public class StampCouponUsageEntity extends BaseEntity {
    private Long stampId;
    private Long issuedCouponId;
    private LocalDateTime usedAt;

    protected StampCouponUsageEntity() {
    }

    private StampCouponUsageEntity(Long stampId, Long issuedCouponId, LocalDateTime usedAt) {
        this.stampId = stampId;
        this.issuedCouponId = issuedCouponId;
        this.usedAt = usedAt;
    }

    public static StampCouponUsageEntity from(StampCouponUsage stampCouponUsage) {
        return new StampCouponUsageEntity(stampCouponUsage.getStampId(), stampCouponUsage.getIssuedCouponId(), stampCouponUsage.getUsedAt());
    }

    public StampCouponUsage toModel() {
        return new StampCouponUsage(id, stampId, issuedCouponId, usedAt);
    }

}
