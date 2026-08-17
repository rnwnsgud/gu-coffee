package com.coffee.gu.stamp;

import com.coffee.gu.Principal;
import com.coffee.gu.StampEarnEvent;
import com.coffee.gu.enums.StampState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StampRewardPlannerTest {

    @Mock
    private StampRepository stampRepository;

    @InjectMocks
    private StampRewardPlanner stampRewardPlanner;

    @Test
    @DisplayName("사용 가능한 스탬프가 25개일 경우, 20개의 스탬프를 사용하여 2개의 쿠폰을 발급하는 StampRewardPlan을 수립한다")
    void plan_SuccessWith20Stamps() {
        // given
        Principal principal = Principal.user("U100");
        StampEarnEvent event = new StampEarnEvent(principal, 1L, "강남점");

        given(stampRepository.countAvailableStamps(anyString(), any()))
                .willReturn(25L);

        List<Stamp> mockStamps = LongStream.rangeClosed(1, 20)
                .mapToObj(i -> new Stamp(i, "ORDER-KEY", principal, StampState.EARNED, LocalDateTime.now(), LocalDateTime.now().plusDays(30)))
                .toList();

        given(stampRepository.getAvailableStamps(anyString(), any(), anyInt(), anyInt()))
                .willReturn(mockStamps);

        // when
        StampRewardPlan plan = stampRewardPlanner.plan(event);

        // then
        assertThat(plan.isEmpty()).isFalse();
        assertThat(plan.getCouponIssueCount()).isEqualTo(2);
        assertThat(plan.getStampUseCount()).isEqualTo(20);
        assertThat(plan.getStampsToUse()).hasSize(20);
    }

    @Test
    @DisplayName("사용 가능한 스탬프가 10개 미만(9개)일 경우 빈 StampRewardPlan을 반환한다")
    void plan_EmptyWhenLessThan10Stamps() {
        // given
        Principal principal = Principal.user("U100");
        StampEarnEvent event = new StampEarnEvent(principal, 1L, "강남점");

        given(stampRepository.countAvailableStamps(anyString(), any()))
                .willReturn(9L);

        // when
        StampRewardPlan plan = stampRewardPlanner.plan(event);

        // then
        assertThat(plan.isEmpty()).isTrue();
        assertThat(plan.getCouponIssueCount()).isEqualTo(0);
    }
}
