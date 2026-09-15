package com.coffee.gu.stamp

import java.time.LocalDateTime

class StampCouponUsage(
    val id: Long? = 0,
    val stampId: Long? = 0,
    val issuedCouponId: Long? = 0,
    val usedAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
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
