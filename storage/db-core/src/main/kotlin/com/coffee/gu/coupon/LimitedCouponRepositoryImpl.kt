package com.coffee.gu.coupon

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
class LimitedCouponRepositoryImpl(
    private val limitedCouponJpaRepository: LimitedCouponJpaRepository,
    private val couponJpaRepository: CouponJpaRepository,
) : LimitedCouponRepository {

    override fun findById(id: Long): Optional<LimitedCoupon> {
        return limitedCouponJpaRepository.findById(id).map { it.toModel() }
    }

    override fun save(limitedCoupon: LimitedCoupon): LimitedCoupon {
        val couponEntity = couponJpaRepository.findById(limitedCoupon.coupon.id)
            .orElseThrow { CoreException(ErrorType.COUPON_NOT_FOUND_OR_EXPIRED) }
        val entity = LimitedCouponEntity.from(limitedCoupon, couponEntity)
        return limitedCouponJpaRepository.save(entity).toModel()
    }
}
