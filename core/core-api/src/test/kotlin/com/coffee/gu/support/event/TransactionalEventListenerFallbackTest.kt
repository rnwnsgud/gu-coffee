package com.coffee.gu.support.event

import com.coffee.gu.Principal
import com.coffee.gu.StampEarnEvent
import com.coffee.gu.TestApplication
import com.coffee.gu.enums.PrincipalType
import com.coffee.gu.stamp.messaging.StampMessagingProducer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootTest(classes = [TestApplication::class])
@Import(TransactionalEventListenerFallbackTest.NoFallbackListener::class)
@ActiveProfiles("local")
class TransactionalEventListenerFallbackTest {

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    private lateinit var noFallbackListener: NoFallbackListener

    @MockitoBean
    private lateinit var stampMessagingProducer: StampMessagingProducer

    @Component
    class NoFallbackListener {
        var latch: CountDownLatch = CountDownLatch(1)

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
        fun handle(event: StampEarnEvent) {
            latch.countDown()
        }
    }

    @Test
    @DisplayName("[버그 재현] fallbackExecution = false 이면 트랜잭션 없는 스레드(스케줄러 등)에서 발행 시 이벤트가 침묵 무시(Drop)된다")
    fun reproduce_silent_failure_without_fallback() {
        // given: 활성 트랜잭션이 전혀 없는 일반 스레드 환경
        val event = StampEarnEvent(Principal("U1", PrincipalType.USER), 1L, "강남점")

        // when: 트랜잭션 없이 발행
        eventPublisher.publishEvent(event)

        // then: fallbackExecution = false 인 리스너는 1초를 기다려도 절대 호출되지 않음!
        val called = noFallbackListener.latch.await(1, TimeUnit.SECONDS)
        assertThat(called).`as`("트랜잭션이 없으므로 리스너가 실행되지 않아야 한다").isFalse()
    }

    @Test
    @DisplayName("[안전성 검증] fallbackExecution = true 가 적용된 StampEventListener는 트랜잭션이 없어도 정상 실행된다")
    fun verify_stamp_event_listener_works_without_transaction() {
        // given: 활성 트랜잭션이 없는 일반 스레드 환경
        val event = StampEarnEvent(Principal("U1", PrincipalType.USER), 1L, "강남점")

        // when: 트랜잭션 없이 발행
        eventPublisher.publishEvent(event)

        // then: StampEventListener는 비동기로 즉시 실행되어 stampMessagingProducer.send()를 정상 호출함!
        verify(stampMessagingProducer, timeout(3000)).send(event)
    }
}
