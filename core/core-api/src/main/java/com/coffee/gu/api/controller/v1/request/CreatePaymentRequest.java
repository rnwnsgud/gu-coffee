package com.coffee.gu.api.controller.v1.request;

import com.coffee.gu.coupon.IssuedCoupon;
import com.coffee.gu.payment.PaymentDiscount;

import java.math.BigDecimal;
import java.util.List;

public record CreatePaymentRequest(
        String orderKey,
        Long usedIssuedCouponId
) {
    public PaymentDiscount toPaymentDiscount(List<IssuedCoupon> issuedCoupons, BigDecimal orderAmount) {
        return PaymentDiscount.of(issuedCoupons, usedIssuedCouponId, orderAmount);
    }
}
