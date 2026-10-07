package com.coffee.gu.coupon

import java.util.Optional

interface LimitedCouponRepository {
    fun findById(id: Long): Optional<LimitedCoupon>
    fun save(limitedCoupon: LimitedCoupon): LimitedCoupon
}
