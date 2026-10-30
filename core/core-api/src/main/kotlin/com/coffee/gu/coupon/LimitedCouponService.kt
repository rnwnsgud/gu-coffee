package com.coffee.gu.coupon

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.LimitedCouponIssueEvent
import com.coffee.gu.Principal
import com.coffee.gu.coupon.messaging.LimitedCouponIssueProducer
import com.coffee.gu.lock.LimitedCouponClaimer
import com.coffee.gu.lock.LockKeyGenerator
import com.coffee.gu.lock.LockManager
import org.springframework.stereotype.Service

@Service
class LimitedCouponService(
    private val limitedCouponFinder: LimitedCouponFinder,
    private val limitedCouponIssueExecutor: LimitedCouponIssueExecutor,
    private val lockManager: LockManager,
    private val limitedCouponClaimer: LimitedCouponClaimer,
    private val limitedCouponIssueProducer: LimitedCouponIssueProducer,
) {
    fun get(limitedCouponId: Long): LimitedCoupon {
        return limitedCouponFinder.getById(limitedCouponId)
    }

    fun issue(principal: Principal, limitedCouponId: Long): IssuedCoupon {
        val lockKey = LockKeyGenerator.generateLimitedCouponKey(limitedCouponId)
        return lockManager.executeWithLock(lockKey) {
            limitedCouponIssueExecutor.execute(principal, limitedCouponId)
        }
    }

    fun issueAsync(principal: Principal, limitedCouponId: Long): LimitedCouponClaimReceipt {
        val claimResult = limitedCouponClaimer.claim(limitedCouponId, principal.key) {
            val limitedCoupon = limitedCouponFinder.getById(limitedCouponId)
            limitedCoupon.remainingQuantity
        }

        return when (claimResult) {
            LimitedCouponClaimer.ClaimResult.ALREADY_CLAIMED -> {
                throw CoreException(ErrorType.COUPON_ALREADY_DOWNLOADED)
            }
            LimitedCouponClaimer.ClaimResult.SOLD_OUT -> {
                throw CoreException(ErrorType.LIMITED_COUPON_SOLD_OUT)
            }
            LimitedCouponClaimer.ClaimResult.SUCCESS -> {
                val event = LimitedCouponIssueEvent(
                    principal = principal,
                    limitedCouponId = limitedCouponId,
                )
                limitedCouponIssueProducer.send(event)
                LimitedCouponClaimReceipt(event.eventId)
            }
        }
    }
}
