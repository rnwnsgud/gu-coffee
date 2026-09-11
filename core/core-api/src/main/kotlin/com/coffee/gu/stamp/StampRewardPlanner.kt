package com.coffee.gu.stamp

import com.coffee.gu.StampEarnEvent
import com.coffee.gu.coupon.Coupon
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class StampRewardPlanner(
    private val stampRepository: StampRepository,
) {
    fun plan(event: StampEarnEvent): StampRewardPlan {
        val availableCount = stampRepository.countAvailableStamps(
            event.principal.key,
            LocalDateTime.now()
        ).toInt()

        val couponIssueCount = availableCount / Coupon.REWARD_COUPON_STAMP_COUNT

        if (couponIssueCount <= 0) {
            return StampRewardPlan.empty(event.principal)
        }

        val stampUseCount = couponIssueCount * Coupon.REWARD_COUPON_STAMP_COUNT

        val stampsToUse = stampRepository.getAvailableStamps(
            event.principal.key,
            LocalDateTime.now(),
            0,
            stampUseCount
        )

        return StampRewardPlan(
            event.principal,
            couponIssueCount,
            stampUseCount,
            stampsToUse
        )
    }
}
