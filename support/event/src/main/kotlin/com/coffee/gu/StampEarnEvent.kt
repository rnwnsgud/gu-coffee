package com.coffee.gu

import com.coffee.gu.enums.EventType
import io.hypersistence.tsid.TSID

class StampEarnEvent(
    val principal: Principal,
    val storeId: Long,
    val storeName: String,
    eventId: String = TSID.Factory.getTsid().toString(),
) : Event(EventType.STAMP_EARN, eventId)
