package com.coffee.gu.coupon;

import com.coffee.gu.enums.CouponTargetType;

import java.util.List;

public interface CouponTargetRepository {
    List<CouponTarget> findAllByTargetTypeAndTargetIdIn(CouponTargetType targetType, List<Long> targetIds);
}
