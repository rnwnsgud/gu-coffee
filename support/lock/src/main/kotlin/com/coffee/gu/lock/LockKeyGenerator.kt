package com.coffee.gu.lock

object LockKeyGenerator {
    fun generateCouponDownloadKey(couponId: Long, principalKey: String): String {
        return "COUPON-DOWNLOAD-$couponId-$principalKey"
    }
}
