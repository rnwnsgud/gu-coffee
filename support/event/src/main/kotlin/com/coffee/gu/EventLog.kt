package com.coffee.gu

import com.coffee.gu.enums.EventLogStatus
import com.coffee.gu.enums.EventLogTarget
import com.coffee.gu.enums.EventType
import java.time.LocalDateTime

class EventLog(
    val eventId: String,
    val eventType: EventType,
    val eventLogTarget: EventLogTarget,
    val payload: String,
    val isPublished: Boolean,
    val status: EventLogStatus = EventLogStatus.PENDING,
    @get:JvmName("getRetryCount")
    val retryCount: Int? = 0,
    val createdAt: LocalDateTime,
    val publishedAt: LocalDateTime? = null,
)
