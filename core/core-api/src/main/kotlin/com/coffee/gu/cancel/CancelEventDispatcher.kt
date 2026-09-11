package com.coffee.gu.cancel

import com.coffee.gu.CancelEvent
import com.coffee.gu.EventDispatcher
import com.coffee.gu.enums.EventType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class CancelEventDispatcher(
    private val jsonMapper: JsonMapper,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : EventDispatcher {

    override fun supports(eventType: EventType): Boolean {
        return eventType == EventType.CANCEL
    }

    override fun dispatch(payload: String) {
        val cancelEvent = jsonMapper.readValue(payload, CancelEvent::class.java)
        applicationEventPublisher.publishEvent(cancelEvent)
    }
}
