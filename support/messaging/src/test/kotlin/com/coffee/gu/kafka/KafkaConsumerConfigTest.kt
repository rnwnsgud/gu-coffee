package com.coffee.gu.kafka

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

class KafkaConsumerConfigTest {

    @Test
    @DisplayName("KafkaConsumerConfig는 지정된 concurrency(3) 및 RECORD AckMode로 리스너 컨테이너 팩토리를 생성한다")
    fun kafkaListenerContainerFactory_concurrency() {
        // given
        val config = KafkaConsumerConfig("localhost:9092", 3)
        val consumerFactory: ConsumerFactory<String, Any> = mock()

        // when
        val factory = config.kafkaListenerContainerFactory(consumerFactory)

        // then
        // 리플렉션 또는 getter로 concurrency 및 ackMode 검증
        val concurrencyField = factory.javaClass.getDeclaredField("concurrency")
        concurrencyField.isAccessible = true
        val concurrencyValue = concurrencyField.get(factory) as Int

        assertThat(concurrencyValue).isEqualTo(3)
        assertThat(factory.containerProperties.ackMode).isEqualTo(ContainerProperties.AckMode.RECORD)
    }
}
