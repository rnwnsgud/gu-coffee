package com.coffee.gu.coupon

import com.coffee.gu.enums.CouponType
import java.math.BigDecimal
import java.time.LocalDateTime

class Coupon(
    val id: Long = 0,
    val name: String,
    val type: CouponType,
    val discount: BigDecimal,
    val expiredAt: LocalDateTime,
) {
    val isFreeDrinkReward: Boolean
        get() = type == CouponType.FREE_DRINK

    fun calculateDiscount(orderAmount: BigDecimal): BigDecimal {
        if (isFreeDrinkReward) {
            return orderAmount.min(REWARD_COUPON_DISCOUNT_AMOUNT)
        }
        return discount
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Coupon) return false
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        const val REWARD_COUPON_MASTER_ID = 1L
        const val REWARD_COUPON_NAME = "리워드 쿠폰"
        const val REWARD_COUPON_STAMP_COUNT = 10
        const val REWARD_COUPON_EXPIRY_DAYS = 30
        val REWARD_COUPON_DISCOUNT_AMOUNT: BigDecimal = BigDecimal("1800")

        @JvmStatic
        fun rewardCoupon(): Coupon {
            return Coupon(
                id = REWARD_COUPON_MASTER_ID,
                name = REWARD_COUPON_NAME,
                type = CouponType.FREE_DRINK,
                discount = REWARD_COUPON_DISCOUNT_AMOUNT,
                expiredAt = LocalDateTime.now().plusDays(REWARD_COUPON_EXPIRY_DAYS.toLong())
            )
        }
    }
}
