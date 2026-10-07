package com.coffee.gu.coupon

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.enums.PrincipalType
import org.springframework.stereotype.Component

@Component
class LimitedCouponManager(
    private val limitedCouponRepository: LimitedCouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
) {
    fun save(limitedCoupon: LimitedCoupon): LimitedCoupon {
        return limitedCouponRepository.save(limitedCoupon)
    }

    fun issue(principal: Principal, limitedCoupon: LimitedCoupon): IssuedCoupon {
        if (principal.type == PrincipalType.GUEST) {
            throw CoreException(ErrorType.UNAUTHORIZED, null)
        }
        val issuedCoupon = limitedCoupon.createIssuedCoupon(principal)
        return issuedCouponRepository.save(issuedCoupon)
    }
}
