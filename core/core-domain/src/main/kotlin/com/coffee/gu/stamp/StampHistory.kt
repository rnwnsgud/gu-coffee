package com.coffee.gu.stamp

import com.coffee.gu.Principal
import com.coffee.gu.enums.StampHistoryType
import java.time.LocalDateTime

class StampHistory(
    val id: Long = 0,
    val principal: Principal,
    val type: StampHistoryType,
    val storeId: Long,
    val storeName: String,
    val quantity: Int,
    val recordedAt: LocalDateTime = LocalDateTime.now(),
    val expiredAt: LocalDateTime? = null,
) {
    companion object {
        fun createEarnHistory(
            principal: Principal,
            storeId: Long,
            storeName: String,
            quantity: Int,
            recordedAt: LocalDateTime = LocalDateTime.now(),
            expiredAt: LocalDateTime? = null,
        ): StampHistory {
            return StampHistory(
                id = 0,
                principal = principal,
                type = StampHistoryType.EARNED,
                storeId = storeId,
                storeName = storeName,
                quantity = quantity,
                recordedAt = recordedAt,
                expiredAt = expiredAt,
            )
        }

        fun createUseHistory(
            principal: Principal,
            storeId: Long,
            storeName: String,
            quantity: Int,
        ): StampHistory {
            return StampHistory(
                id = 0,
                principal = principal,
                type = StampHistoryType.USED,
                storeId = storeId,
                storeName = storeName,
                quantity = quantity,
                expiredAt = null,
            )
        }

        fun createCancelHistory(
            principal: Principal,
            storeId: Long,
            storeName: String,
            quantity: Int,
        ): StampHistory {
            return StampHistory(
                id = 0,
                principal = principal,
                type = StampHistoryType.CANCELED,
                storeId = storeId,
                storeName = storeName,
                quantity = quantity,
                expiredAt = null,
            )
        }
    }
}
