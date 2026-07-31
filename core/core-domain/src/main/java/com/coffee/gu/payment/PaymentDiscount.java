package com.coffee.gu.payment;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.coupon.Coupon;
import com.coffee.gu.coupon.IssuedCoupon;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record PaymentDiscount(
        List<IssuedCoupon> issuedCoupons,
        Long useIssuedCouponId,
        BigDecimal orderAmount,
        BigDecimal couponDiscount
) {

    public PaymentDiscount {
        if (couponDiscount == null) {
            couponDiscount = BigDecimal.ZERO;
        }
        BigDecimal paidAmount = orderAmount.subtract(couponDiscount);
        if (paidAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.PAYMENT_INVALID_AMOUNT, null);
        }
    }

    public static PaymentDiscount of(List<IssuedCoupon> issuedCoupons, Long useIssuedCouponId, BigDecimal orderAmount) {
        if (useIssuedCouponId == null || useIssuedCouponId <= 0) {
            return new PaymentDiscount(issuedCoupons, useIssuedCouponId, orderAmount, BigDecimal.ZERO);
        }
        BigDecimal calculatedDiscount = issuedCoupons.stream()
                .filter(issuedCoupon -> Objects.equals(issuedCoupon.getId(), useIssuedCouponId))
                .map(issuedCoupon -> {
                    Coupon coupon = issuedCoupon.getCoupon();
                    return coupon.calculateDiscount(orderAmount);
                })
                .findFirst()
                .orElseThrow(() -> new CoreException(ErrorType.ISSUED_COUPON_INVALID, null));

        return new PaymentDiscount(issuedCoupons, useIssuedCouponId, orderAmount, calculatedDiscount);
    }

    public BigDecimal getPaidAmount() {
        return orderAmount.subtract(couponDiscount);
    }

    public boolean hasUsedCoupon() {
        return useIssuedCouponId != null && useIssuedCouponId > 0;
    }
}
