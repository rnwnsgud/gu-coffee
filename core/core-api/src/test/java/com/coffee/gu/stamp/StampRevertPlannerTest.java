package com.coffee.gu.stamp;

import com.coffee.gu.Principal;
import com.coffee.gu.coupon.Coupon;
import com.coffee.gu.coupon.IssuedCoupon;
import com.coffee.gu.coupon.IssuedCouponRepository;
import com.coffee.gu.enums.IssuedCouponState;
import com.coffee.gu.enums.OrderState;
import com.coffee.gu.enums.StampState;
import com.coffee.gu.order.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StampRevertPlannerTest {

    @Mock
    private StampRepository stampRepository;

    @Mock
    private StampCouponUsageRepository stampCouponUsageRepository;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @InjectMocks
    private StampRevertPlanner stampRevertPlanner;

    @Test
    @DisplayName("주문 취소 시 소진된(USED) 스탬프가 존재하면 연관된 리워드 쿠폰을 역추적하여 StampRevertPlan을 수립한다")
    void plan_WithUsedStampsAndRewardCoupons() {
        // given
        Principal principal = Principal.user("U100");
        Order order = new Order("ORDER-100", "아메리카노 1잔", principal, 1L, BigDecimal.TEN, OrderState.PAID, List.of());

        Stamp usedStamp = new Stamp(1L, "ORDER-100", principal, StampState.USED, LocalDateTime.now(), LocalDateTime.now().plusDays(30));

        given(stampRepository.findByOrderKey("ORDER-100"))
                .willReturn(List.of(usedStamp));

        StampCouponUsage usage = StampCouponUsage.create(1L, 500L, LocalDateTime.now());
        given(stampCouponUsageRepository.findAllByStampIdIn(anyList()))
                .willReturn(List.of(usage));

        IssuedCoupon rewardCoupon = new IssuedCoupon(500L, principal, IssuedCouponState.DOWNLOADED, Coupon.rewardCoupon());
        given(issuedCouponRepository.findAllByIdIn(anyList()))
                .willReturn(List.of(rewardCoupon));

        // when
        StampRevertPlan plan = stampRevertPlanner.plan(order);

        // then
        assertThat(plan.isEmpty()).isFalse();
        assertThat(plan.cancelStampCount()).isEqualTo(1);
        assertThat(plan.hasRewardCouponsToCancel()).isTrue();
        assertThat(plan.getRewardCouponsToCancel()).containsExactly(rewardCoupon);
    }
}
