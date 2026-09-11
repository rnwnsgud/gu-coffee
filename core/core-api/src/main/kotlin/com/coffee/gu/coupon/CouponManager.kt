package com.coffee.gu.coupon

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.enums.PrincipalType
import org.springframework.stereotype.Component

@Component
class CouponManager(
    private val issuedCouponRepository: IssuedCouponRepository
) {
    fun issue(principal: Principal, coupon: Coupon) {
        if (principal.type == PrincipalType.GUEST) throw CoreException(ErrorType.UNAUTHORIZED, null)
        issuedCouponRepository.save(IssuedCoupon.download(principal, coupon))
    }
}
