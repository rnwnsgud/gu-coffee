package com.coffee.gu.stamp

import com.coffee.gu.Principal
import com.coffee.gu.enums.StampState
import java.time.LocalDateTime
import java.time.LocalTime

class Stamp(
    val id: Long = 0,
    val orderKey: String,
    val principal: Principal,
    var state: StampState,
    val createdAt: LocalDateTime,
    val expiredAt: LocalDateTime,
) {
    fun use() {
        this.state = StampState.USED
    }

    fun cancel() {
        this.state = StampState.CANCELED
    }

    companion object {
        const val EXPIRY_ALARM_DAYS = 30L
        const val EXPIRY_DAYS = 180L

        fun create(
            principal: Principal,
            orderKey: String,
            now : LocalDateTime = LocalDateTime.now(),
        ): Stamp {
            return Stamp(
                id = 0,
                orderKey = orderKey,
                principal = principal,
                state = StampState.EARNED,
                createdAt = now,
                expiredAt = now.plusDays(EXPIRY_DAYS).toLocalDate().atTime(LocalTime.MAX)
            )
        }
    }
}
