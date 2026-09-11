package com.coffee.gu.coupon

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.enums.IssuedCouponState
import org.springframework.stereotype.Component

@Component
class IssuedCouponFinder(
    private val issuedCouponRepository: IssuedCouponRepository
) {
    fun findAllByPrincipalKey(principalKey: String): List<IssuedCoupon> {
        return issuedCouponRepository.findAllByPrincipalKey(principalKey)
    }

    fun existsByPrincipalKeyAndCouponId(principal: Principal, couponId: Long): Boolean {
        return issuedCouponRepository.existsByPrincipalKeyAndCouponId(principal.key, couponId)
    }

    fun getUsableAllByPrincipalAndCoupons(principal: Principal, coupons: List<Coupon>): List<IssuedCoupon> {
        return issuedCouponRepository.findAllByPrincipalKey(principal.key)
            .filter { issuedCoupon -> issuedCoupon.state == IssuedCouponState.DOWNLOADED }
            .filter { issuedCoupon -> coupons.contains(issuedCoupon.coupon) }
    }

    fun getById(issuedCouponId: Long): IssuedCoupon {
        return issuedCouponRepository.findById(issuedCouponId)
    }
}
