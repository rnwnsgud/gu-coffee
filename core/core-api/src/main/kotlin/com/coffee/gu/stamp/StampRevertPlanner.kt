package com.coffee.gu.stamp

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.coupon.IssuedCoupon
import com.coffee.gu.coupon.IssuedCouponRepository
import com.coffee.gu.enums.StampState
import com.coffee.gu.order.Order
import org.springframework.stereotype.Component

@Component
class StampRevertPlanner(
    private val stampRepository: StampRepository,
    private val stampCouponUsageRepository: StampCouponUsageRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
) {
    fun plan(order: Order): StampRevertPlan {
        val stamps = stampRepository.findByOrderKey(order.key)
        val stampsToCancel = stamps.filter { it.state != StampState.CANCELED }
        if (stampsToCancel.isEmpty()) {
            return StampRevertPlan.empty()
        }
        val usedStamps = stampsToCancel.filter { it.state == StampState.USED }
        val rewardCouponsToCancel = findRewardCouponsIssuedBy(usedStamps)
        return StampRevertPlan(
            stampsToCancel,
            usedStamps,
            rewardCouponsToCancel
        )
    }

    private fun findRewardCouponsIssuedBy(usedStamps: List<Stamp>): List<IssuedCoupon> {
        val usedStampIds = usedStamps.map { it.id }
        val usages = stampCouponUsageRepository.findAllByStampIdIn(usedStampIds)
        if (usages.isEmpty()) throw CoreException(ErrorType.NOT_FOUND_DATA)
        val issuedCouponIds = usages.mapNotNull { it.issuedCouponId }.distinct()
        val issuedCoupons = issuedCouponRepository.findAllByIdIn(issuedCouponIds)
        if (issuedCoupons.size != issuedCouponIds.size) {
            throw CoreException(ErrorType.NOT_FOUND_DATA, null)
        }
        return issuedCoupons
    }
}
