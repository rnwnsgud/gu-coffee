package com.coffee.gu.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler

class KafkaConsumerConfigTest {

    @Test
    @DisplayName("KafkaConsumerConfig는 auto.commit을 비활성화하고 RECORD AckMode 및 DLT 에러 핸들러가 포함된 리스너 컨테이너 팩토리를 생성한다")
    fun kafkaListenerContainerFactory_concurrency() {
        // given
        val config = KafkaConsumerConfig("localhost:9092", 3)
        val consumerFactory: ConsumerFactory<String, Any> = mock()
        val kafkaTemplate: KafkaTemplate<String, Any> = mock()
        val recoverer = config.deadLetterPublishingRecoverer(kafkaTemplate)
        val errorHandler = config.kafkaCommonErrorHandler(recoverer)

        // when
        val realConsumerFactory = config.consumerFactory()
        val factory = config.kafkaListenerContainerFactory(consumerFactory, errorHandler)

        // then
        assertThat(realConsumerFactory.configurationProperties[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG]).isEqualTo(false)
        assertThat(errorHandler).isInstanceOf(DefaultErrorHandler::class.java)

        val concurrencyField = factory.javaClass.getDeclaredField("concurrency")
        concurrencyField.isAccessible = true
        val concurrencyValue = concurrencyField.get(factory) as Int

        assertThat(concurrencyValue).isEqualTo(3)
        assertThat(factory.containerProperties.ackMode).isEqualTo(ContainerProperties.AckMode.RECORD)
    }
}
