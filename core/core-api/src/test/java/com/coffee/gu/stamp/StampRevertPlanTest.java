package com.coffee.gu.stamp;

import com.coffee.gu.Principal;
import com.coffee.gu.coupon.Coupon;
import com.coffee.gu.coupon.IssuedCoupon;
import com.coffee.gu.enums.IssuedCouponState;
import com.coffee.gu.enums.StampState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StampRevertPlanTest {

    @Test
    @DisplayName("회수 대상 리워드 쿠폰 중 이미 사용된(USED) 쿠폰이 존재하는 경우 hasUsedRewardCoupon()은 true를 반환한다")
    void hasUsedRewardCoupon_True() {
        // given
        Principal principal = Principal.user("U100");
        Stamp stamp = new Stamp(1L, "ORDER-KEY", principal, StampState.USED, LocalDateTime.now(), LocalDateTime.now().plusDays(30));
        IssuedCoupon usedCoupon = new IssuedCoupon(101L, principal, IssuedCouponState.USED, Coupon.rewardCoupon());

        StampRevertPlan plan = new StampRevertPlan(List.of(stamp), List.of(stamp), List.of(usedCoupon));

        // when & then
        assertThat(plan.hasUsedRewardCoupon()).isTrue();
    }

    @Test
    @DisplayName("회수 대상 리워드 쿠폰이 모두 미사용(DOWNLOADED) 상태인 경우 hasUsedRewardCoupon()은 false를 반환한다")
    void hasUsedRewardCoupon_False() {
        // given
        Principal principal = Principal.user("U100");
        Stamp stamp = new Stamp(1L, "ORDER-KEY", principal, StampState.USED, LocalDateTime.now(), LocalDateTime.now().plusDays(30));
        IssuedCoupon downloadedCoupon = new IssuedCoupon(101L, principal, IssuedCouponState.DOWNLOADED, Coupon.rewardCoupon());

        StampRevertPlan plan = new StampRevertPlan(List.of(stamp), List.of(stamp), List.of(downloadedCoupon));

        // when & then
        assertThat(plan.hasUsedRewardCoupon()).isFalse();
    }
}
