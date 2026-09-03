package com.coffee.gu.stamp

interface StampCouponUsageRepository {
    fun save(stampCouponUsage: StampCouponUsage): StampCouponUsage
    fun saveAll(stampCouponUsages: List<StampCouponUsage>): List<StampCouponUsage>
    fun findAllByStampIdIn(stampIds: List<Long>): List<StampCouponUsage>
}
