package com.coffee.gu

import com.coffee.gu.enums.EventType

class StampEarnEvent(
    val principal: Principal,
    val storeId: Long,
    val storeName: String
) : Event(EventType.STAMP_EARN)
