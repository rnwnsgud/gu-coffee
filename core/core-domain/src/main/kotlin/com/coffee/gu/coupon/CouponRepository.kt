package com.coffee.gu.coupon

interface CouponRepository {
    fun findAllByIdIn(couponIds: List<Long>): List<Coupon>
    fun findById(couponId: Long): Coupon?
}
