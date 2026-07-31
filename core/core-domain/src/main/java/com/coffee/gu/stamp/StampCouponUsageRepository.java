package com.coffee.gu.stamp;

import java.util.List;

public interface StampCouponUsageRepository {
    StampCouponUsage save(StampCouponUsage stampCouponUsage);
    List<StampCouponUsage> saveAll(List<StampCouponUsage> stampCouponUsages);
    List<StampCouponUsage> findAllByStampIdIn(List<Long> stampIds);
}
