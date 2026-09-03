package com.coffee.gu.stamp

import java.time.LocalDateTime

class StampCouponUsage(
    val id: Long = 0,
    val stampId: Long,
    val issuedCouponId: Long,
    val usedAt: LocalDateTime,
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
