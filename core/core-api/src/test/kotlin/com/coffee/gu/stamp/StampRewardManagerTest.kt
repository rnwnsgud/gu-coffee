package com.coffee.gu.stamp

import com.coffee.gu.EventLogRepository
import com.coffee.gu.Principal
import com.coffee.gu.StampEarnEvent
import com.coffee.gu.coupon.IssuedCouponRepository
import com.coffee.gu.event.OutboxEventPublisher
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class StampRewardManagerTest {

    @Mock
    private lateinit var stampRewardPlanner: StampRewardPlanner

    @Mock
    private lateinit var stampRepository: StampRepository

    @Mock
    private lateinit var issuedCouponRepository: IssuedCouponRepository

    @Mock
    private lateinit var eventLogRepository: EventLogRepository

    @Mock
    private lateinit var stampRecorder: StampRecorder

    @Mock
    private lateinit var outboxEventPublisher: OutboxEventPublisher

    @InjectMocks
    private lateinit var stampRewardManager: StampRewardManager

    @Test
    @DisplayName("DB 커밋 후 Kafka 오프셋 커밋 전 장애로 인해 이미 처리 완료된(isPublished=true) 이벤트가 재수신되면 중복 실행을 즉시 건너뛴다")
    fun stampToCouponWithIdempotency_skipsAlreadyPublishedEvent() {
        // given
        val principal = Principal.user("U100")
        val event = StampEarnEvent(principal, 1L, "강남점")
        given(eventLogRepository.isPublished(event.eventId)).willReturn(true)

        // when
        stampRewardManager.stampToCouponWithIdempotency(event)

        // then
        verify(stampRewardPlanner, never()).plan(event)
        verify(eventLogRepository, never()).publish(event)
    }
}
