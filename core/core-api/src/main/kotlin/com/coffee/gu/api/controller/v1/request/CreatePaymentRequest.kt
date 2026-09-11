package com.coffee.gu.api.controller.v1.request

import com.coffee.gu.coupon.IssuedCoupon
import com.coffee.gu.payment.PaymentDiscount
import java.math.BigDecimal

class CreatePaymentRequest(
    val orderKey: String,
    val usedIssuedCouponId: Long?,
) {
    fun toPaymentDiscount(issuedCoupons: List<IssuedCoupon>, orderAmount: BigDecimal): PaymentDiscount {
        return PaymentDiscount.of(issuedCoupons, usedIssuedCouponId, orderAmount)
    }
}
