package com.coffee.gu.coupon

interface IssuedCouponRepository {
    fun save(issuedCoupon: IssuedCoupon): IssuedCoupon
    fun saveAll(issuedCoupons: List<IssuedCoupon>): List<IssuedCoupon>
    fun findAllByPrincipalKey(principalKey: String): List<IssuedCoupon>
    fun existsByPrincipalKeyAndCouponId(principalKey: String, couponId: Long): Boolean
    fun findById(issuedCouponId: Long): IssuedCoupon?
    fun findAllByIdIn(issuedCouponIds: List<Long>): List<IssuedCoupon>
}
