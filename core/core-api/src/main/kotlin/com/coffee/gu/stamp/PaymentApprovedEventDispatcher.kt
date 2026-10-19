package com.coffee.gu.stamp

import com.coffee.gu.EventDispatcher
import com.coffee.gu.PaymentApprovedEvent
import com.coffee.gu.enums.EventType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class PaymentApprovedEventDispatcher(
    private val jsonMapper: JsonMapper,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : EventDispatcher {

    override fun supports(eventType: EventType): Boolean {
        return eventType == EventType.PAYMENT_APPROVED
    }

    override fun dispatch(payload: String) {
        val paymentApprovedEvent = jsonMapper.readValue(payload, PaymentApprovedEvent::class.java)
        applicationEventPublisher.publishEvent(paymentApprovedEvent)
    }
}
