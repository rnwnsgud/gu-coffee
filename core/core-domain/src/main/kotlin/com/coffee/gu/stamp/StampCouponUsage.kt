package com.coffee.gu.stamp

import java.time.LocalDateTime

class StampCouponUsage @JvmOverloads constructor(
    val id: Long? = 0,
    val stampId: Long? = 0,
    val issuedCouponId: Long? = 0,
    val usedAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        @JvmStatic
        fun create(
            stampId: Long,
            issuedCouponId: Long,
            now: LocalDateTime,
        ): StampCouponUsage {
            return StampCouponUsage(
                id = 0,
                stampId = stampId,
                issuedCouponId = issuedCouponId,
                usedAt = now,
            )
        }
    }
}
