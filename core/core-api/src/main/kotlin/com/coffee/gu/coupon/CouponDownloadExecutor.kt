package com.coffee.gu.coupon

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponDownloadExecutor(
    private val couponFinder: CouponFinder,
    private val issuedCouponFinder: IssuedCouponFinder,
    private val couponManager: CouponManager,
) {
    @Transactional
    fun download(principal: Principal, couponId: Long) {
        val coupon = couponFinder.getValidCoupon(couponId)
        val exist = issuedCouponFinder.existsByPrincipalKeyAndCouponId(principal, couponId)
        if (exist) throw CoreException(ErrorType.COUPON_ALREADY_DOWNLOADED, null)
        couponManager.issue(principal, coupon)
    }
}
