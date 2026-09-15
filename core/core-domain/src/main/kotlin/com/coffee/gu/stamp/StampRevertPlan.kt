package com.coffee.gu.stamp

import com.coffee.gu.coupon.IssuedCoupon

class StampRevertPlan(
    val stampsToCancel: List<Stamp>,
    val usedStamps: List<Stamp>,
    val rewardCouponsToCancel: List<IssuedCoupon>,
) {
    val isEmpty: Boolean
        get() = stampsToCancel.isEmpty()

    val hasRewardCouponsToCancel: Boolean
        get() = rewardCouponsToCancel.isNotEmpty()

    val hasUsedRewardCoupon: Boolean
        get() = rewardCouponsToCancel.any { it.isUsed }

    fun hasRewardCouponsToCancel(): Boolean = hasRewardCouponsToCancel
    fun hasUsedRewardCoupon(): Boolean = hasUsedRewardCoupon

    fun cancelStampCount(): Int {
        return stampsToCancel.size
    }

    companion object {
        fun empty(): StampRevertPlan {
            return StampRevertPlan(
                stampsToCancel = emptyList(),
                usedStamps = emptyList(),
                rewardCouponsToCancel = emptyList(),
            )
        }
    }
}
