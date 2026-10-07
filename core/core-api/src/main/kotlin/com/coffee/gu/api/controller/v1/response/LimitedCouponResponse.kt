package com.coffee.gu.api.controller.v1.response

import com.coffee.gu.coupon.LimitedCoupon
import com.coffee.gu.enums.CouponType
import java.math.BigDecimal
import java.time.LocalDateTime

data class LimitedCouponResponse(
    val id: Long,
    val name: String,
    val type: CouponType,
    val discount: BigDecimal,
    val expiredAt: LocalDateTime,
    val totalQuantity: Int,
    val issuedQuantity: Int,
    val remainingQuantity: Int,
) {
    companion object {
        fun from(limitedCoupon: LimitedCoupon): LimitedCouponResponse {
            return LimitedCouponResponse(
                id = limitedCoupon.id,
                name = limitedCoupon.coupon.name,
                type = limitedCoupon.coupon.type,
                discount = limitedCoupon.coupon.discount,
                expiredAt = limitedCoupon.coupon.expiredAt,
                totalQuantity = limitedCoupon.totalQuantity,
                issuedQuantity = limitedCoupon.issuedQuantity,
                remainingQuantity = limitedCoupon.remainingQuantity,
            )
        }
    }
}
