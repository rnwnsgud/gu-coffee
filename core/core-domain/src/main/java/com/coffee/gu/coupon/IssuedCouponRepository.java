package com.coffee.gu.coupon;

import java.util.List;
import java.util.Optional;

public interface IssuedCouponRepository {
    IssuedCoupon save(IssuedCoupon issuedCoupon);
    List<IssuedCoupon> saveAll(List<IssuedCoupon> issuedCoupons);
    List<IssuedCoupon> findAllByPrincipalKey(String principalKey);
    boolean existsByPrincipalKeyAndCouponId(String principalKey, Long couponId);
    Optional<IssuedCoupon> findById(Long issuedCouponId);
    List<IssuedCoupon> findAllByIdIn(List<Long> issuedCouponIds);

}
