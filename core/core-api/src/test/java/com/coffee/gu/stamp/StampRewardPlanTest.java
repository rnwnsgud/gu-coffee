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
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

class StampRewardPlanTest {

    @Test
    @DisplayName("20개의 스탬프를 사용하여 2개의 쿠폰을 발급할 때, 10개씩 subList로 슬라이싱하여 StampCouponUsage 이력을 정확히 20개 생성한다")
    void createStampCouponUsages_SlicingTest() {
        // given
        Principal principal = Principal.user("U100");
        List<Stamp> stamps = LongStream.rangeClosed(1, 20)
                .mapToObj(i -> new Stamp(i, "ORDER-KEY", principal, StampState.EARNED, LocalDateTime.now(), LocalDateTime.now().plusDays(30)))
                .toList();

        IssuedCoupon coupon1 = new IssuedCoupon(101L, principal, IssuedCouponState.DOWNLOADED, Coupon.rewardCoupon());
        IssuedCoupon coupon2 = new IssuedCoupon(102L, principal, IssuedCouponState.DOWNLOADED, Coupon.rewardCoupon());
        List<IssuedCoupon> issuedCoupons = List.of(coupon1, coupon2);

        StampRewardPlan plan = new StampRewardPlan(principal, 2, 20, stamps);

        // when
        List<StampCouponUsage> usages = plan.createStampCouponUsages(issuedCoupons, stamps);

        // then
        assertThat(usages).hasSize(20);

        // 0~9번 스탬프(10개)는 coupon1(id: 101)에 매핑되었는지 검증
        List<StampCouponUsage> firstCouponUsages = usages.subList(0, 10);
        assertThat(firstCouponUsages).allMatch(usage -> usage.getIssuedCouponId().equals(101L));

        // 10~19번 스탬프(10개)는 coupon2(id: 102)에 매핑되었는지 검증
        List<StampCouponUsage> secondCouponUsages = usages.subList(10, 20);
        assertThat(secondCouponUsages).allMatch(usage -> usage.getIssuedCouponId().equals(102L));
    }

    @Test
    @DisplayName("스탬프 사용 수량이 0이거나 쿠폰 발급 수량이 0인 경우 isEmpty()는 true를 반환한다")
    void isEmpty_Test() {
        // given
        Principal principal = Principal.user("U100");
        StampRewardPlan emptyPlan = StampRewardPlan.empty(principal);

        // when & then
        assertThat(emptyPlan.isEmpty()).isTrue();
    }
}
