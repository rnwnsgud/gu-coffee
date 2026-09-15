package com.coffee.gu.payment

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.coupon.IssuedCoupon
import java.math.BigDecimal

class PaymentDiscount(
    val issuedCoupons: List<IssuedCoupon>,
    val useIssuedCouponId: Long? = null,
    val orderAmount: BigDecimal,
    var couponDiscount: BigDecimal = BigDecimal.ZERO,
) {
    val paidAmount: BigDecimal
        get() = orderAmount - couponDiscount

    init {
        if (paidAmount < BigDecimal.ZERO) {
            throw CoreException(ErrorType.PAYMENT_INVALID_AMOUNT, null)
        }
    }

    val hasUsedCoupon: Boolean
        get() = useIssuedCouponId != null && useIssuedCouponId > 0

    companion object {
        fun of(
            issuedCoupons: List<IssuedCoupon>,
            useIssuedCouponId: Long?,
            orderAmount: BigDecimal,
        ): PaymentDiscount {
            if (useIssuedCouponId == null || useIssuedCouponId <= 0) {
                return PaymentDiscount(
                    issuedCoupons = issuedCoupons,
                    useIssuedCouponId = useIssuedCouponId,
                    orderAmount = orderAmount,
                    couponDiscount = BigDecimal.ZERO,
                )
            }

            val issuedCoupon = issuedCoupons.firstOrNull { it.id == useIssuedCouponId }
                ?: throw CoreException(ErrorType.ISSUED_COUPON_INVALID)
            val calculatedDiscount = issuedCoupon.coupon.calculateDiscount(orderAmount)


            return PaymentDiscount(
                issuedCoupons = issuedCoupons,
                useIssuedCouponId = useIssuedCouponId,
                orderAmount = orderAmount,
                couponDiscount = calculatedDiscount,
            )
        }
    }
}