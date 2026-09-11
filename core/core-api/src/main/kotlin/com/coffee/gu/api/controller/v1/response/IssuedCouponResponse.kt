package com.coffee.gu.api.controller.v1.response

import com.coffee.gu.coupon.IssuedCoupon
import com.coffee.gu.enums.CouponType
import com.coffee.gu.enums.IssuedCouponState
import java.math.BigDecimal
import java.time.LocalDateTime

class IssuedCouponResponse(
    val id: Long,
    val state: IssuedCouponState,
    val name: String,
    val type: CouponType,
    val discount: BigDecimal,
    val expiredAt: LocalDateTime,
) {
    companion object {
        @JvmStatic
        fun from(issuedCoupon: IssuedCoupon): IssuedCouponResponse {
            return IssuedCouponResponse(
                id = issuedCoupon.id,
                state = issuedCoupon.state,
                name = issuedCoupon.coupon.name,
                type = issuedCoupon.coupon.type,
                discount = issuedCoupon.coupon.discount,
                expiredAt = issuedCoupon.coupon.expiredAt
            )
        }

        @JvmStatic
        fun from(issuedCoupons: List<IssuedCoupon>): List<IssuedCouponResponse> {
            return issuedCoupons.map { from(it) }
        }
    }
}
