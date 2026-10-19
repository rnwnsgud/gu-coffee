package com.coffee.gu.stamp.messaging

import com.coffee.gu.Principal
import com.coffee.gu.StampEarnEvent
import com.coffee.gu.kafka.KafkaTopic
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture

class StampMessagingProducerTest {

    private val kafkaTemplate: KafkaTemplate<String, Any> = mock()
    private val producer = StampMessagingProducer(kafkaTemplate)

    @Test
    @DisplayName("스탬프 적립 이벤트를 사용자 키를 파티션 키로 하여 메시징 브로커에 발행한다")
    fun send_success() {
        // given
        val principal = Principal.user("123")
        val event = StampEarnEvent(principal, 1L, "강남점")

        val recordMetadata = RecordMetadata(TopicPartition(KafkaTopic.STAMP_EARN_V1, 0), 0L, 0, 0L, 0, 0)
        val producerRecord = ProducerRecord<String, Any>(KafkaTopic.STAMP_EARN_V1, principal.key, event)
        val sendResult = SendResult(producerRecord, recordMetadata)
        val future = CompletableFuture.completedFuture(sendResult)

        `when`(kafkaTemplate.send(KafkaTopic.STAMP_EARN_V1, principal.key, event)).thenReturn(future)

        // when
        producer.send(event)

        // then
        verify(kafkaTemplate).send(KafkaTopic.STAMP_EARN_V1, principal.key, event)
    }
}
