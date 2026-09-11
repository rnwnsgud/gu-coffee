package com.coffee.gu.api.controller.v1.response.stamp

import com.coffee.gu.stamp.StampHistory
import com.coffee.gu.stamp.StampHistoryItem

class StampHistoryResponse(
    val expiringSoonCount: Int,
    val histories: List<StampHistoryItem>,
) {
    companion object {
        @JvmStatic
        fun of(expiringSoonCount: Int, stampHistories: List<StampHistory>): StampHistoryResponse {
            val stampHistoryItems = stampHistories.map { StampHistoryItem.from(it) }
            return StampHistoryResponse(expiringSoonCount, stampHistoryItems)
        }
    }
}
