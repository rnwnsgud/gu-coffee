package com.coffee.gu.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer
import org.springframework.util.backoff.FixedBackOff

@Configuration
@EnableKafka
class KafkaConsumerConfig(
    @param:Value("\${spring.kafka.bootstrap-servers:localhost:9092}")
    private val bootstrapServers: String,
    @param:Value("\${spring.kafka.listener.concurrency:3}")
    private val concurrency: Int,
    @param:Value("\${spring.kafka.listener.auto-startup:true}")
    private val autoStartup: Boolean,
) {
    companion object {
        const val RETRY_INTERVAL_MS = 1000L
        const val MAX_RETRY_ATTEMPTS = 3L
    }

    @Bean
    @ConditionalOnMissingBean
    fun consumerFactory(): ConsumerFactory<String, Any> {
        val configProps = mutableMapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JacksonJsonDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            JacksonJsonDeserializer.TRUSTED_PACKAGES to "com.coffee.gu,com.coffee.gu.*",
        )
        return DefaultKafkaConsumerFactory(configProps)
    }

    @Bean
    @ConditionalOnMissingBean
    fun deadLetterPublishingRecoverer(
        kafkaTemplate: KafkaTemplate<String, Any>,
    ): DeadLetterPublishingRecoverer {
        return DeadLetterPublishingRecoverer(kafkaTemplate) { record, _ ->
            TopicPartition(record.topic() + KafkaTopic.DLT_SUFFIX, record.partition())
        }
    }

    @Bean
    @ConditionalOnMissingBean
    fun kafkaCommonErrorHandler(
        deadLetterPublishingRecoverer: DeadLetterPublishingRecoverer,
    ): CommonErrorHandler {
        return DefaultErrorHandler(
            deadLetterPublishingRecoverer,
            FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRY_ATTEMPTS),
        )
    }

    @Bean
    @ConditionalOnMissingBean
    @Suppress("UsePropertyAccessSyntax")
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, Any>,
        kafkaCommonErrorHandler: CommonErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.setConsumerFactory(consumerFactory)
        factory.setConcurrency(concurrency)
        factory.setAutoStartup(autoStartup)
        factory.setCommonErrorHandler(kafkaCommonErrorHandler)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        return factory
    }
}
