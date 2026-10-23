package com.coffee.gu.coupon

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class LimitedCouponRepositoryImpl(
    private val limitedCouponJpaRepository: LimitedCouponJpaRepository,
    private val couponJpaRepository: CouponJpaRepository,
) : LimitedCouponRepository {

    override fun findById(id: Long): LimitedCoupon? {
        val limitedCoupon = limitedCouponJpaRepository.findByIdOrNull(id) ?: return null
        val coupon = couponJpaRepository.findByIdOrNull(limitedCoupon.couponId)
            ?: throw CoreException(ErrorType.COUPON_NOT_FOUND_OR_EXPIRED)
        return limitedCoupon.toModel(coupon.toModel())
    }

    override fun findByIdWithLock(id: Long): LimitedCoupon? {
        val limitedCoupon = limitedCouponJpaRepository.findByIdForUpdate(id) ?: return null
        val coupon = couponJpaRepository.findByIdOrNull(limitedCoupon.couponId)
            ?: throw CoreException(ErrorType.COUPON_NOT_FOUND_OR_EXPIRED)
        return limitedCoupon.toModel(coupon.toModel())
    }

    override fun save(limitedCoupon: LimitedCoupon): LimitedCoupon {
        val coupon = couponJpaRepository.findByIdOrNull(limitedCoupon.coupon.id)
            ?: throw CoreException(ErrorType.COUPON_NOT_FOUND_OR_EXPIRED)
        val savedLimitedCoupon = limitedCouponJpaRepository.save(LimitedCouponEntity.from(limitedCoupon))
        return savedLimitedCoupon.toModel(coupon.toModel())
    }
}
