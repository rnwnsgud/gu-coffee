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

    fun issueToPrincipal(principal: Principal, limitedCoupon: LimitedCoupon): IssuedCoupon {
        if (principal.type == PrincipalType.GUEST) {
            throw CoreException(ErrorType.UNAUTHORIZED, null)
        }
        val issuedCoupon = limitedCoupon.createIssuedCoupon(principal)
        return issuedCouponRepository.save(issuedCoupon)
    }

    @Deprecated("명확한 행위 표현을 위해 issueToPrincipal() 사용을 권장합니다.", ReplaceWith("issueToPrincipal(principal, limitedCoupon)"))
    fun issue(principal: Principal, limitedCoupon: LimitedCoupon): IssuedCoupon = issueToPrincipal(principal, limitedCoupon)
}
