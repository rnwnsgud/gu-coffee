package com.coffee.gu.stamp

import com.coffee.gu.Principal
import com.coffee.gu.coupon.Coupon
import com.coffee.gu.coupon.IssuedCoupon
import java.time.LocalDateTime

class StampRewardPlan @JvmOverloads constructor(
    val principal: Principal,
    val couponIssueCount: Long,
    val stampUseCount: Long,
    val stampsToUse: List<Stamp>,
    var issuedCoupons: List<IssuedCoupon> = emptyList(),
) {
    val isEmpty: Boolean
        get() = couponIssueCount <= 0 || stampsToUse.isEmpty()

    fun assignIssuedCoupons(issuedCoupons: List<IssuedCoupon>) {
        this.issuedCoupons = issuedCoupons
    }

    fun createStampCouponUsages(
        issuedCoupons: List<IssuedCoupon>,
        stamps: List<Stamp>,
    ): List<StampCouponUsage> {
        val usages = mutableListOf<StampCouponUsage>()
        val stampCountPerCoupon = Coupon.REWARD_COUPON_STAMP_COUNT

        for (i in issuedCoupons.indices) {
            val issuedCoupon = issuedCoupons[i]
            val fromIndex = i * stampCountPerCoupon
            val toIndex = fromIndex + stampCountPerCoupon

            val couponStamps = stamps.subList(fromIndex, toIndex)
            val now = LocalDateTime.now()

            for (stamp in couponStamps) {
                usages.add(StampCouponUsage.create(stamp.id, issuedCoupon.id, now))
            }
        }

        return usages
    }

    companion object {
        @JvmStatic
        fun empty(principal: Principal): StampRewardPlan {
            return StampRewardPlan(
                principal = principal,
                couponIssueCount = 0,
                stampUseCount = 0,
                stampsToUse = emptyList(),
            )
        }
    }
}
