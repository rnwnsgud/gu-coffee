package com.coffee.gu.stamp

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.coupon.IssuedCoupon
import com.coffee.gu.coupon.IssuedCouponRepository
import com.coffee.gu.order.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class StampRevertManager(
    private val stampRevertPlanner: StampRevertPlanner,
    private val stampRepository: StampRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
    private val stampRecorder: StampRecorder,
) {
    fun validateRevertable(order: Order) {
        val plan = stampRevertPlanner.plan(order)
        validate(plan)
    }

    @Transactional
    fun revert(order: Order) {
        val plan = stampRevertPlanner.plan(order)
        if (plan.isEmpty) return
        validate(plan)
        cancelRewardCoupons(plan)
        cancelStamps(plan)
        stampRecorder.recordCancel(order, plan)
    }

    private fun validate(plan: StampRevertPlan) {
        if (plan.hasUsedRewardCoupon()) {
            throw CoreException(
                ErrorType.INVALID_REQUEST,
                "이미 사용된 리워드 쿠폰이 있어 주문을 취소할 수 없습니다."
            )
        }
    }

    private fun cancelRewardCoupons(plan: StampRevertPlan) {
        if (!plan.hasRewardCouponsToCancel()) return
        plan.rewardCouponsToCancel.forEach { it.cancel() }
        issuedCouponRepository.saveAll(plan.rewardCouponsToCancel)
    }

    private fun cancelStamps(plan: StampRevertPlan) {
        plan.stampsToCancel.forEach { it.cancel() }
        stampRepository.saveAll(plan.stampsToCancel)
    }
}
