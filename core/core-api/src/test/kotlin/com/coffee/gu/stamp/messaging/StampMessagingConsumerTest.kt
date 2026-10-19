package com.coffee.gu.stamp.messaging

import com.coffee.gu.Principal
import com.coffee.gu.StampEarnEvent
import com.coffee.gu.stamp.StampHandler
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class StampMessagingConsumerTest {

    private val stampHandler: StampHandler = mock()
    private val consumer = StampMessagingConsumer(stampHandler)

    @Test
    @DisplayName("이벤트를 수신하면 StampHandler의 멱등 적립 메서드를 정상 호출한다")
    fun consume_success() {
        // given
        val event = StampEarnEvent(Principal.user("123"), 1L, "강남점")

        // when
        consumer.consume(event)

        // then
        verify(stampHandler).stampToCouponWithIdempotency(event)
    }

    @Test
    @DisplayName("StampHandler 처리 중 예외가 발생하면 예외를 상위로 전파한다")
    fun consume_error_propagation() {
        // given
        val event = StampEarnEvent(Principal.user("123"), 1L, "강남점")
        doThrow(RuntimeException("DB Lock timeout")).`when`(stampHandler).stampToCouponWithIdempotency(event)

        // when & then
        assertThrows<RuntimeException> {
            consumer.consume(event)
        }
        verify(stampHandler).stampToCouponWithIdempotency(event)
    }
}
