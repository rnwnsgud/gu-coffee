package com.coffee.gu.coupon

interface LimitedCouponRepository {
    fun findById(id: Long): LimitedCoupon?
    fun findByIdWithLock(id: Long): LimitedCoupon?
    fun save(limitedCoupon: LimitedCoupon): LimitedCoupon
}
