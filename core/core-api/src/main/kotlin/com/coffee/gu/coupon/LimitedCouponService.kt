package com.coffee.gu.coupon

import com.coffee.gu.Principal
import com.coffee.gu.lock.LockKeyGenerator
import com.coffee.gu.lock.LockManager
import org.springframework.stereotype.Service

@Service
class LimitedCouponService(
    private val limitedCouponFinder: LimitedCouponFinder,
    private val limitedCouponIssueExecutor: LimitedCouponIssueExecutor,
    private val lockManager: LockManager,
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
}
