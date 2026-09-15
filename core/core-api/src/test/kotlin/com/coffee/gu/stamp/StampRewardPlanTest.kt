package com.coffee.gu.stamp

import com.coffee.gu.Principal
import com.coffee.gu.coupon.Coupon
import com.coffee.gu.coupon.IssuedCoupon
import com.coffee.gu.enums.IssuedCouponState
import com.coffee.gu.enums.StampState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class StampRewardPlanTest {

    @Test
    @DisplayName("20개의 스탬프를 사용하여 2개의 쿠폰을 발급할 때, 10개씩 subList로 슬라이싱하여 StampCouponUsage 이력을 정확히 20개 생성한다")
    fun createStampCouponUsages_SlicingTest() {
        // given
        val principal = Principal.user("U100")
        val stamps = (1L..20L).map { i ->
            Stamp(
                id = i,
                orderKey = "ORDER-KEY",
                principal = principal,
                state = StampState.EARNED,
                createdAt = LocalDateTime.now(),
                expiredAt = LocalDateTime.now().plusDays(30)
            )
        }

        val coupon1 = IssuedCoupon(
            id = 101L,
            principal = principal,
            state = IssuedCouponState.DOWNLOADED,
            coupon = Coupon.rewardCoupon()
        )
        val coupon2 = IssuedCoupon(
            id = 102L,
            principal = principal,
            state = IssuedCouponState.DOWNLOADED,
            coupon = Coupon.rewardCoupon()
        )
        val issuedCoupons = listOf(coupon1, coupon2)

        val plan = StampRewardPlan(principal, 2, 20, stamps)

        // when
        val usages = plan.createStampCouponUsages(issuedCoupons, stamps)

        // then
        assertThat(usages).hasSize(20)

        // 0~9번 스탬프(10개)는 coupon1(id: 101)에 매핑되었는지 검증
        val firstCouponUsages = usages.subList(0, 10)
        assertThat(firstCouponUsages).allMatch { it.issuedCouponId == 101L }

        // 10~19번 스탬프(10개)는 coupon2(id: 102)에 매핑되었는지 검증
        val secondCouponUsages = usages.subList(10, 20)
        assertThat(secondCouponUsages).allMatch { it.issuedCouponId == 102L }
    }

    @Test
    @DisplayName("스탬프 사용 수량이 0이거나 쿠폰 발급 수량이 0인 경우 isEmpty()는 true를 반환한다")
    fun isEmpty_Test() {
        // given
        val principal = Principal.user("U100")
        val emptyPlan = StampRewardPlan.empty(principal)

        // when & then
        assertThat(emptyPlan.isEmpty).isTrue()
    }
}
