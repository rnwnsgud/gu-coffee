package com.coffee.gu.stamp

import java.time.LocalDateTime

interface StampHistoryRepository {
    fun save(stampHistory: StampHistory): StampHistory
    fun getWithinNMonths(principalKey: String, nMonthsAgo: LocalDateTime): List<StampHistory>
}
