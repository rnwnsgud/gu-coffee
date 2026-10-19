package com.coffee.gu.stamp.messaging

import com.coffee.gu.Principal
import com.coffee.gu.StampEarnEvent
import com.coffee.gu.TestApplication
import com.coffee.gu.kafka.KafkaTopic
import com.coffee.gu.stamp.StampHandler
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JacksonJsonSerializer
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest(classes = [TestApplication::class])
@EmbeddedKafka(
    partitions = 3,
    topics = [KafkaTopic.STAMP_EARN_V1],
    bootstrapServersProperty = "spring.kafka.bootstrap-servers",
)
@ActiveProfiles("local")
@Import(StampMessagingIntegrationTest.RealKafkaTestConfig::class)
@DirtiesContext
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class StampMessagingIntegrationTest {

    @Autowired
    private lateinit var producer: StampMessagingProducer

    @MockitoBean
    private lateinit var stampHandler: StampHandler

    @BeforeEach
    fun setUp(
        @Autowired registry: KafkaListenerEndpointRegistry,
        @Autowired embeddedKafka: EmbeddedKafkaBroker,
    ) {
        registry.listenerContainers.forEach { container ->
            ContainerTestUtils.waitForAssignment(container, embeddedKafka.partitionsPerTopic)
        }
    }

    @TestConfiguration
    class RealKafkaTestConfig {

        @Bean
        @Primary
        fun kafkaTemplate(embeddedKafka: EmbeddedKafkaBroker): KafkaTemplate<String, Any> {
            val props = mutableMapOf<String, Any>(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to embeddedKafka.brokersAsString,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JacksonJsonSerializer::class.java,
                ProducerConfig.ACKS_CONFIG to "all",
            )
            return KafkaTemplate(DefaultKafkaProducerFactory(props))
        }
    }

    @Test
    @DisplayName("E2E: StampMessagingProducer가 이벤트를 발행하면 EmbeddedKafka 브로커를 거쳐 StampMessagingConsumer가 수신하고 스탬프 멱등 적립을 처리한다")
    fun produce_and_consume_e2e() {
        // given
        val principal = Principal.user("12345")
        val event = StampEarnEvent(principal, 10L, "강남본점")

        // when: 프로듀서가 카프카 토픽으로 발행
        producer.send(event)

        // then: 임베디드 카프카 브로커를 통해 컨슈머가 수신하여 stampHandler 호출 검증 (최대 7초 대기)
        verify(stampHandler, timeout(7000).times(1)).stampToCouponWithIdempotency(event)
    }

    @Test
    @DisplayName("E2E: 동일한 이벤트가 중복 발행되어도 컨슈머가 정상 수신하여 멱등 적립 메서드를 2회 호출한다 (도메인 멱등성 보장)")
    fun duplicate_produce_and_consume_e2e() {
        // given
        val principal = Principal.user("99999")
        val event = StampEarnEvent(principal, 10L, "강남본점")

        // when: 동일한 이벤트 2회 발행
        producer.send(event)
        producer.send(event)

        // then: 컨슈머는 2회 모두 수신하여 stampHandler에 안전하게 위임함을 검증 (최대 7초 대기)
        verify(stampHandler, timeout(7000).times(2)).stampToCouponWithIdempotency(event)
    }
}
