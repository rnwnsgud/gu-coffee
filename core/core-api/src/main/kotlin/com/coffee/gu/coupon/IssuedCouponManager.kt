package com.coffee.gu.coupon

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import org.springframework.stereotype.Component

@Component
class IssuedCouponManager(
    private val issuedCouponRepository: IssuedCouponRepository,
    private val issuedCouponFinder: IssuedCouponFinder
) {
    fun use(principal: Principal, issuedCouponId: Long?) {
        if (issuedCouponId == null) return
        val issuedCoupon = issuedCouponFinder.getById(issuedCouponId)
        if (issuedCoupon.principal != principal) throw CoreException(ErrorType.UNAUTHORIZED, null)
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
