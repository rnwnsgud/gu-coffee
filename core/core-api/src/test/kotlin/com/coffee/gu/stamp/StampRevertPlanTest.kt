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

class StampRevertPlanTest {

    @Test
    @DisplayName("회수 대상 리워드 쿠폰 중 이미 사용된(USED) 쿠폰이 존재하는 경우 hasUsedRewardCoupon()은 true를 반환한다")
    fun hasUsedRewardCoupon_True() {
        // given
        val principal = Principal.user("U100")
        val stamp = Stamp(
            id = 1L,
            orderKey = "ORDER-KEY",
            principal = principal,
            state = StampState.USED,
            createdAt = LocalDateTime.now(),
            expiredAt = LocalDateTime.now().plusDays(30)
        )
        val usedCoupon = IssuedCoupon(
            id = 101L,
            principal = principal,
            state = IssuedCouponState.USED,
            coupon = Coupon.rewardCoupon()
        )

        val plan = StampRevertPlan(listOf(stamp), listOf(stamp), listOf(usedCoupon))

        // when & then
        assertThat(plan.hasUsedRewardCoupon()).isTrue()
    }

    @Test
    @DisplayName("회수 대상 리워드 쿠폰이 모두 미사용(DOWNLOADED) 상태인 경우 hasUsedRewardCoupon()은 false를 반환한다")
    fun hasUsedRewardCoupon_False() {
        // given
        val principal = Principal.user("U100")
        val stamp = Stamp(
            id = 1L,
            orderKey = "ORDER-KEY",
            principal = principal,
            state = StampState.USED,
            createdAt = LocalDateTime.now(),
            expiredAt = LocalDateTime.now().plusDays(30)
        )
        val downloadedCoupon = IssuedCoupon(
            id = 101L,
            principal = principal,
            state = IssuedCouponState.DOWNLOADED,
            coupon = Coupon.rewardCoupon()
        )

        val plan = StampRevertPlan(listOf(stamp), listOf(stamp), listOf(downloadedCoupon))

        // when & then
        assertThat(plan.hasUsedRewardCoupon()).isFalse()
    }
}
