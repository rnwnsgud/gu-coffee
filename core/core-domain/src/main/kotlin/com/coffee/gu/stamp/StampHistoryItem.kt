package com.coffee.gu.stamp

import com.coffee.gu.enums.StampHistoryType
import java.time.LocalDateTime

class StampHistoryItem(
    val type: StampHistoryType,
    val displayDate: LocalDateTime,
    val quantity: Long,
    val storeName: String,
    val expiredAt: LocalDateTime?,
) {
    companion object {
        @JvmStatic
        fun from(stampHistory: StampHistory): StampHistoryItem {
            return StampHistoryItem(
                type = stampHistory.type,
                displayDate = stampHistory.recordedAt,
                quantity = stampHistory.quantity,
                storeName = stampHistory.storeName,
                expiredAt = stampHistory.expiredAt,
            )
        }
    }
}
