package com.coffee.gu.api.controller.v1.response;

import com.coffee.gu.coupon.IssuedCoupon;
import com.coffee.gu.enums.CouponType;
import com.coffee.gu.enums.IssuedCouponState;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record IssuedCouponResponse(
        Long id,
        IssuedCouponState state,
        String name,
        CouponType type,
        BigDecimal discount,
        LocalDateTime expiredAt
) {
    public static IssuedCouponResponse from(IssuedCoupon issuedCoupon) {
        return new IssuedCouponResponse(
                issuedCoupon.getId(),
                issuedCoupon.getState(),
                issuedCoupon.getCoupon().getName(),
                issuedCoupon.getCoupon().getType(),
                issuedCoupon.getCoupon().getDiscount(),
                issuedCoupon.getCoupon().getExpiredAt()
        );
    }

    public static List<IssuedCouponResponse> from(List<IssuedCoupon> issuedCoupons) {
        return issuedCoupons.stream()
                .map(IssuedCouponResponse::from)
                .toList();
    }
}
