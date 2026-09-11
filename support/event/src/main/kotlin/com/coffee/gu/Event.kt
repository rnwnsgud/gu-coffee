package com.coffee.gu

import com.coffee.gu.enums.EventType
import io.hypersistence.tsid.TSID

abstract class Event(
    val eventType: EventType,
    val eventId: String = TSID.Factory.getTsid().toString()
)
