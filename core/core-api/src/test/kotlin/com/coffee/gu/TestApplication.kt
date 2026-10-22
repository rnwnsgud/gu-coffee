package com.coffee.gu

import com.coffee.gu.config.CoreDataSourceConfig
import com.coffee.gu.config.CoreJpaConfig
import com.coffee.gu.config.QuerydslConfig
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.client.RestClient
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

@Import(CoreDataSourceConfig::class, CoreJpaConfig::class, QuerydslConfig::class)
@SpringBootApplication(scanBasePackages = ["com.coffee.gu"])
@ConfigurationPropertiesScan(basePackages = ["com.coffee.gu"])
@EnableAsync
@EntityScan(basePackages = ["com.coffee.gu"])
@EnableJpaRepositories(basePackages = ["com.coffee.gu"])
class TestApplication {
    @Bean
    fun restClient(): RestClient {
        return RestClient.create()
    }

    @Bean(name = ["stampAsyncExecutor"])
    fun stampAsyncExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 10
        executor.setThreadNamePrefix("StampAsync-")
        executor.initialize()
        return executor
    }

    @Bean
    @Primary
    @ConditionalOnProperty(
        name = ["spring.kafka.listener.auto-startup"],
        havingValue = "false",
        matchIfMissing = true,
    )
    @Suppress("UNCHECKED_CAST")
    fun mockKafkaTemplate(): KafkaTemplate<String, Any> {
        val template = Mockito.mock(KafkaTemplate::class.java) as KafkaTemplate<String, Any>
        val sendResult = Mockito.mock(SendResult::class.java) as SendResult<String, Any>
        val metadata = RecordMetadata(TopicPartition("mock-topic", 0), 0L, 0, 0L, 0, 0)
        Mockito.`when`(sendResult.recordMetadata).thenReturn(metadata)
        Mockito.`when`(template.send(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any()))
            .thenReturn(CompletableFuture.completedFuture(sendResult))
        Mockito.`when`(template.send(ArgumentMatchers.anyString(), ArgumentMatchers.any()))
            .thenReturn(CompletableFuture.completedFuture(sendResult))
        return template
    }
}
