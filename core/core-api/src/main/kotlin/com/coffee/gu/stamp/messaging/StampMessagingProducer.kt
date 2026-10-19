package com.coffee.gu.stamp.messaging

import com.coffee.gu.StampEarnEvent
import com.coffee.gu.kafka.KafkaTopic
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component

@Component
class StampMessagingProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) {
    companion object {
        private val log = LoggerFactory.getLogger(StampMessagingProducer::class.java)
    }

    fun send(event: StampEarnEvent) {
        val partitionKey = event.principal.key

        kafkaTemplate.send(KafkaTopic.STAMP_EARN_V1, partitionKey, event)
            .whenComplete { result: SendResult<String, Any>?, ex: Throwable? ->
                if (ex == null && result != null) {
                    val metadata = result.recordMetadata
                    log.info(
                        "StampEarnEvent 메시징 발행 성공: eventId={}, partition={}, offset={}",
                        event.eventId,
                        metadata.partition(),
                        metadata.offset(),
                    )
                } else {
                    log.error(
                        "StampEarnEvent 메시징 발행 실패 (Outbox 복구 스케줄러가 재발행 예정): eventId={}",
                        event.eventId,
                        ex,
                    )
                }
            }
    }
}
