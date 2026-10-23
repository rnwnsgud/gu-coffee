package com.coffee.gu.coupon

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import org.springframework.stereotype.Component

@Component
class LimitedCouponFinder(
    private val limitedCouponRepository: LimitedCouponRepository,
) {
    fun getById(id: Long): LimitedCoupon {
        return limitedCouponRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
    }

    fun getByIdWithLock(id: Long): LimitedCoupon {
        return limitedCouponRepository.findByIdWithLock(id)
            ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
    }
}
