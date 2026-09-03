package com.coffee.gu.stamp

import com.coffee.gu.Principal
import com.coffee.gu.enums.StampState
import java.time.LocalDateTime

class Stamp(
    val id: Long = 0,
    val orderKey: String,
    val principal: Principal,
    var state: StampState,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val expiredAt: LocalDateTime? = null,
) {
    fun use() {
        this.state = StampState.USED
    }

    fun cancel() {
        this.state = StampState.CANCELED
    }

    companion object {
        const val EXPIRY_ALARM_DAYS = 30
        const val EXPIRY_DAYS = 180

        @JvmStatic
        fun create(
            principal: Principal,
            orderKey: String,
            expiredAt: LocalDateTime?,
        ): Stamp {
            return Stamp(
                id = 0,
                orderKey = orderKey,
                principal = principal,
                state = StampState.EARNED,
                expiredAt = expiredAt,
            )
        }
    }
}
