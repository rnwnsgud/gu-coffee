package com.coffee.gu.stamp

import com.coffee.gu.EventDispatcher
import com.coffee.gu.StampEarnEvent
import com.coffee.gu.enums.EventType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class StampEventDispatcher(
    private val jsonMapper: JsonMapper,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : EventDispatcher {

    override fun supports(eventType: EventType): Boolean {
        return eventType == EventType.STAMP_EARN
    }

    override fun dispatch(payload: String) {
        val stampEarnEvent = jsonMapper.readValue(payload, StampEarnEvent::class.java)
        applicationEventPublisher.publishEvent(stampEarnEvent)
    }
}
