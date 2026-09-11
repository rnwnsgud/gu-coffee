package com.coffee.gu

import com.coffee.gu.enums.EventType

class CancelEvent(
    val orderKey: String
) : Event(EventType.CANCEL)
