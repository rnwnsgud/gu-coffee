package com.coffee.gu.coupon

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal

class LimitedCoupon(
    val id: Long = 0,
    val coupon: Coupon,
    val totalQuantity: Int,
    val issuedQuantity: Int = 0,
) {
    val isSoldOut: Boolean
        get() = issuedQuantity >= totalQuantity

    val remainingQuantity: Int
        get() = (totalQuantity - issuedQuantity).coerceAtLeast(0)

    fun increaseIssuedQuantity(): LimitedCoupon {
        if (isSoldOut) {
            throw CoreException(ErrorType.LIMITED_COUPON_SOLD_OUT)
        }
        return LimitedCoupon(
            id = this.id,
            coupon = this.coupon,
            totalQuantity = this.totalQuantity,
            issuedQuantity = this.issuedQuantity + 1,
        )
    }

    @Deprecated("명확한 행위 표현을 위해 increaseIssuedQuantity() 사용을 권장합니다.", ReplaceWith("increaseIssuedQuantity()"))
    fun issue(): LimitedCoupon = increaseIssuedQuantity()

    fun createIssuedCoupon(principal: Principal): IssuedCoupon {
        return IssuedCoupon.download(principal, this.coupon)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LimitedCoupon) return false
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
