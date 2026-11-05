package com.coffee.gu.coupon.messaging

import com.coffee.gu.LimitedCouponIssueEvent
import com.coffee.gu.kafka.KafkaTopic
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component

@Component
class LimitedCouponIssueProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) {
    companion object {
        private val log = LoggerFactory.getLogger(LimitedCouponIssueProducer::class.java)
    }

    fun send(event: LimitedCouponIssueEvent) {
        val partitionKey = event.limitedCouponId.toString()

        kafkaTemplate.send(KafkaTopic.LIMITED_COUPON_ISSUE_V1, partitionKey, event)
            .whenComplete { result: SendResult<String, Any>?, ex: Throwable? ->
                if (ex == null && result != null) {
                    val metadata = result.recordMetadata
                    log.info(
                        "LimitedCouponIssueEvent 발행 성공: eventId={}, couponId={}, principalKey={}, partition={}, offset={}",
                        event.eventId,
                        event.limitedCouponId,
                        event.principal.key,
                        metadata.partition(),
                        metadata.offset(),
                    )
                } else {
                    log.error(
                        "LimitedCouponIssueEvent 발행 실패: eventId={}, couponId={}, principalKey={}",
                        event.eventId,
                        event.limitedCouponId,
                        event.principal.key,
                        ex,
                    )
                }
            }
    }
}
