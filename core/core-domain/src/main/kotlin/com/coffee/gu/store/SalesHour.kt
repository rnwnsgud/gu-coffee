package com.coffee.gu.store

import java.time.DayOfWeek
import java.time.LocalTime

@JvmRecord
data class SalesHour(
    val storeId: Long,
    val day: DayOfWeek,
    val open: LocalTime,
    val close: LocalTime,
)
