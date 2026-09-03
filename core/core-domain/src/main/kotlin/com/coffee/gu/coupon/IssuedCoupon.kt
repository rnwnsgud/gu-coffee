package com.coffee.gu.coupon

import com.coffee.gu.Principal
import com.coffee.gu.enums.IssuedCouponState

class IssuedCoupon(
    val id: Long = 0,
    val principal: Principal,
    var state: IssuedCouponState,
    val coupon: Coupon,
) {
    val isUsed: Boolean
        get() = state == IssuedCouponState.USED

    fun use() {
        this.state = IssuedCouponState.USED
    }

    fun revert() {
        this.state = IssuedCouponState.DOWNLOADED
    }

    fun cancel() {
        this.state = IssuedCouponState.CANCELED
    }

    companion object {
        @JvmStatic
        fun download(principal: Principal, coupon: Coupon): IssuedCoupon {
            return IssuedCoupon(
                id = 0,
                principal = principal,
                state = IssuedCouponState.DOWNLOADED,
                coupon = coupon
            )
        }
    }
}
