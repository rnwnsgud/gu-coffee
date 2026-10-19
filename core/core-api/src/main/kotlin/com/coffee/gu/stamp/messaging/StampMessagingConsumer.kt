package com.coffee.gu.stamp.messaging

import com.coffee.gu.StampEarnEvent
import com.coffee.gu.kafka.KafkaTopic
import com.coffee.gu.stamp.StampHandler
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class StampMessagingConsumer(
    private val stampHandler: StampHandler,
) {
    companion object {
        private val log = LoggerFactory.getLogger(StampMessagingConsumer::class.java)
    }

    @KafkaListener(
        topics = [KafkaTopic.STAMP_EARN_V1],
        groupId = "coffee-stamp-group",
    )
    fun consume(event: StampEarnEvent) {
        log.info(
            "StampEarnEvent 메시징 수신: eventId={}, principalKey={}, storeId={}",
            event.eventId,
            event.principal.key,
            event.storeId,
        )
        try {
            stampHandler.stampToCouponWithIdempotency(event)
            log.info("StampEarnEvent 멱등 적립 처리 완료: eventId={}", event.eventId)
        } catch (e: Exception) {
            log.error("StampEarnEvent 처리 중 예외 발생: eventId={}", event.eventId, e)
            throw e
        }
    }
}
