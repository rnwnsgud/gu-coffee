package com.coffee.gu.coupon

import java.util.Optional

interface CouponRepository {
    fun findAllByIdIn(couponIds: List<Long>): List<Coupon>
    fun findById(couponId: Long): Optional<Coupon>
}
