package com.coffee.gu.coupon

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.payment.Payment
import org.springframework.stereotype.Component

@Component
class IssuedCouponManager(
    private val issuedCouponRepository: IssuedCouponRepository,
    private val issuedCouponFinder: IssuedCouponFinder
) {
    fun use(payment: Payment) {
        if (!payment.hasAppliedCoupon()) return
        val issuedCoupon = issuedCouponFinder.getById(payment.issuedCouponId!!)
        if (issuedCoupon.principal != payment.principal) throw CoreException(ErrorType.UNAUTHORIZED, null)
        issuedCoupon.use()
        issuedCouponRepository.save(issuedCoupon)
    }

    fun revert(principal: Principal, issuedCouponId: Long) {
        val issuedCoupon = issuedCouponFinder.getById(issuedCouponId)
        if (issuedCoupon.principal != principal) throw CoreException(ErrorType.UNAUTHORIZED, null)
        issuedCoupon.revert()
        issuedCouponRepository.save(issuedCoupon)
    }
}
