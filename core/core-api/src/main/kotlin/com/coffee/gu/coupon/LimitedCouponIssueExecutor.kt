package com.coffee.gu.coupon

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LimitedCouponIssueExecutor(
    private val limitedCouponFinder: LimitedCouponFinder,
    private val issuedCouponFinder: IssuedCouponFinder,
    private val limitedCouponManager: LimitedCouponManager,
) {
    @Transactional
    fun execute(principal: Principal, limitedCouponId: Long): IssuedCoupon {
        val limitedCoupon = limitedCouponFinder.getById(limitedCouponId)
        val alreadyIssued = issuedCouponFinder.existsByPrincipalKeyAndCouponId(principal, limitedCoupon.coupon.id)
        if (alreadyIssued) {
            throw CoreException(ErrorType.COUPON_ALREADY_DOWNLOADED)
        }
        val updatedCoupon = limitedCoupon.issue()
        limitedCouponManager.save(updatedCoupon)
        return limitedCouponManager.issue(principal, updatedCoupon)
    }
}
