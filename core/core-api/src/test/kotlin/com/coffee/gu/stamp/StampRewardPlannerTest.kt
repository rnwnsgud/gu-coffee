package com.coffee.gu.stamp

import com.coffee.gu.Principal
import com.coffee.gu.StampEarnEvent
import com.coffee.gu.enums.StampState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.quality.Strictness
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StampRewardPlannerTest {

    @Mock
    private lateinit var stampRepository: StampRepository

    @InjectMocks
    private lateinit var stampRewardPlanner: StampRewardPlanner

    @Test
    @DisplayName("사용 가능한 스탬프가 25개일 경우, 20개의 스탬프를 사용하여 2개의 쿠폰을 발급하는 StampRewardPlan을 수립한다")
    fun plan_SuccessWith20Stamps() {
        // given
        val principal = Principal.user("U100")
        val event = StampEarnEvent(principal, 1L, "강남점")

        given(stampRepository.countAvailableStamps(any(), any()))
            .willReturn(25L)

        val mockStamps = (1L..20L).map { i ->
            Stamp(i, "ORDER-KEY", principal, StampState.EARNED, LocalDateTime.now(), LocalDateTime.now().plusDays(30))
        }

        given(stampRepository.getAvailableStamps(any(), any(), any(), any()))
            .willReturn(mockStamps)

        // when
        val plan = stampRewardPlanner.plan(event)

        // then
        assertThat(plan.isEmpty).isFalse()
        assertThat(plan.couponIssueCount).isEqualTo(2)
        assertThat(plan.stampUseCount).isEqualTo(20)
        assertThat(plan.stampsToUse).hasSize(20)
    }

    @Test
    @DisplayName("사용 가능한 스탬프가 10개 미만(9개)일 경우 빈 StampRewardPlan을 반환한다")
    fun plan_EmptyWhenLessThan10Stamps() {
        // given
        val principal = Principal.user("U100")
        val event = StampEarnEvent(principal, 1L, "강남점")

        given(stampRepository.countAvailableStamps(any(), any()))
            .willReturn(9L)

        // when
        val plan = stampRewardPlanner.plan(event)

        // then
        assertThat(plan.isEmpty).isTrue()
        assertThat(plan.couponIssueCount).isEqualTo(0)
    }
}
