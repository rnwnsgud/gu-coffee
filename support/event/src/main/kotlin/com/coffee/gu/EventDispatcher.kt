package com.coffee.gu

import com.coffee.gu.enums.EventType

interface EventDispatcher {
    fun supports(eventType: EventType): Boolean
    fun dispatch(payload: String)
}
