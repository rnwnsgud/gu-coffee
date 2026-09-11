package com.coffee.gu.event

import com.coffee.gu.Event
import com.coffee.gu.EventLogRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class OutboxEventPublisher(
    private val eventLogRepository: EventLogRepository,
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    fun publishOutboxEvent(event: Event): Boolean {
        val saved = eventLogRepository.saveIfNotExists(event)
        if (saved) {
            applicationEventPublisher.publishEvent(event)
        }
        return saved
    }
}
