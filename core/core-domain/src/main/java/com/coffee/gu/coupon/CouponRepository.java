package com.coffee.gu.coupon;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    List<Coupon> findAllByIdIn(List<Long> couponIds);
    Optional<Coupon> findById(Long couponId);

}
