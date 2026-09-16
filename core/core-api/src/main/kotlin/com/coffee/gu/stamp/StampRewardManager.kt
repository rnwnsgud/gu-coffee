package com.coffee.gu.stamp

import com.coffee.gu.EventLogRepository
import com.coffee.gu.Principal
import com.coffee.gu.StampEarnEvent
import com.coffee.gu.coupon.Coupon
import com.coffee.gu.coupon.IssuedCoupon
import com.coffee.gu.coupon.IssuedCouponRepository
import com.coffee.gu.event.OutboxEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class StampRewardManager(
    private val stampRewardPlanner: StampRewardPlanner,
    private val stampRepository: StampRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
    private val eventLogRepository: EventLogRepository,
    private val stampRecorder: StampRecorder,
    private val outboxEventPublisher: OutboxEventPublisher,
) {
    @Transactional
    fun reward(principal: Principal, orderKey: String, stampQuantity: Int, expiredAt: LocalDateTime, now: LocalDateTime, storeId: Long) {
        if (stampQuantity <= 0) return
        val stamps = List(stampQuantity) {
            Stamp.create(principal, orderKey, expiredAt)
        }
        stampRepository.saveAll(stamps)
        val store =  stampRecorder.recordEarn(principal, storeId, stampQuantity, now, expiredAt)
        val event = StampEarnEvent(principal, storeId, store.name)
        outboxEventPublisher.publishOutboxEvent(event)
    }

    @Transactional
    fun stampToCouponWithIdempotency(event: StampEarnEvent) {
        val plan = stampRewardPlanner.plan(event)
        if (plan.isEmpty) return
        useStamps(plan)
        issueCoupons(plan)
        stampRecorder.recordStampCouponUsages(plan)
        stampRecorder.recordUse(event, plan.couponIssueCount)
        eventLogRepository.publish(event)
    }

    private fun useStamps(plan: StampRewardPlan) {
        plan.stampsToUse.forEach { it.use() }
        stampRepository.saveAll(plan.stampsToUse)
    }

    private fun issueCoupons(plan: StampRewardPlan) {
        var issuedCoupons = List(plan.couponIssueCount) {
            IssuedCoupon.download(plan.principal, Coupon.rewardCoupon())
        }
        issuedCoupons = issuedCouponRepository.saveAll(issuedCoupons)
        plan.assignIssuedCoupons(issuedCoupons)
    }
}
