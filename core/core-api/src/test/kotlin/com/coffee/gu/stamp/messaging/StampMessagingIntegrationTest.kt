package com.coffee.gu.stamp.messaging

import com.coffee.gu.Principal
import com.coffee.gu.StampEarnEvent
import com.coffee.gu.TestApplication
import com.coffee.gu.kafka.KafkaTopic
import com.coffee.gu.stamp.StampHandler
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer
import org.springframework.kafka.support.serializer.JacksonJsonSerializer
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Duration

@SpringBootTest(classes = [TestApplication::class])
@EmbeddedKafka(
    partitions = 3,
    topics = [KafkaTopic.STAMP_EARN_V1, KafkaTopic.STAMP_EARN_V1_DLT],
    bootstrapServersProperty = "spring.kafka.bootstrap-servers",
)
@ActiveProfiles("local")
@Import(StampMessagingIntegrationTest.RealKafkaTestConfig::class)
@DirtiesContext
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class StampMessagingIntegrationTest {

    @Autowired
    private lateinit var producer: StampMessagingProducer

    @Autowired
    private lateinit var embeddedKafka: EmbeddedKafkaBroker

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

    @Test
    @DisplayName("E2E: 컨슈머 처리 중 예외가 계속 발생하면 최초 1회 + 재시도 3회(총 4회) 시도 후 DLT 토픽으로 메시지를 격리 발행한다")
    fun consume_fail_retries_and_publishes_to_dlt() {
        // given
        val principal = Principal.user("77777")
        val event = StampEarnEvent(principal, 10L, "강남본점")
        doThrow(RuntimeException("DB 일시 장애")).`when`(stampHandler).stampToCouponWithIdempotency(event)

        val dltConsumerProps = KafkaTestUtils.consumerProps(
            embeddedKafka.brokersAsString,
            "dlt-test-group",
            true,
        ).apply {
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer::class.java)
            put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.coffee.gu,com.coffee.gu.*")
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        }
        val dltConsumer = DefaultKafkaConsumerFactory<String, Any>(dltConsumerProps).createConsumer()
        embeddedKafka.consumeFromAnEmbeddedTopic(dltConsumer, KafkaTopic.STAMP_EARN_V1_DLT)

        // when
        producer.send(event)

        // then: 최초 1회 + 재시도 3회 = 총 4회 호출 검증
        verify(stampHandler, timeout(10000).times(4)).stampToCouponWithIdempotency(event)

        // DLT 토픽에 격리된 레코드 수신 검증
        val dltRecords = KafkaTestUtils.getRecords(dltConsumer, Duration.ofSeconds(5))
        assertThat(dltRecords.count()).isGreaterThanOrEqualTo(1)
        val dltRecord = dltRecords.records(KafkaTopic.STAMP_EARN_V1_DLT).first()
        assertThat(dltRecord.key()).isEqualTo(principal.key)
        dltConsumer.close()
    }
}
