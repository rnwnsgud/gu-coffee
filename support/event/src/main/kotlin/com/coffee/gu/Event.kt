package com.coffee.gu

import com.coffee.gu.enums.EventType
import io.hypersistence.tsid.TSID

abstract class Event(
    val eventType: EventType,
    val eventId: String = TSID.Factory.getTsid().toString()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Event) return false
        return eventId == other.eventId
    }

    override fun hashCode(): Int {
        return eventId.hashCode()
    }
}
