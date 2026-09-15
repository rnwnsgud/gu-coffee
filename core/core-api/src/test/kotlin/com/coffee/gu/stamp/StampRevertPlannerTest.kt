package com.coffee.gu.stamp

import com.coffee.gu.Principal
import com.coffee.gu.coupon.Coupon
import com.coffee.gu.coupon.IssuedCoupon
import com.coffee.gu.coupon.IssuedCouponRepository
import com.coffee.gu.enums.IssuedCouponState
import com.coffee.gu.enums.OrderState
import com.coffee.gu.enums.StampState
import com.coffee.gu.order.Order
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
import java.math.BigDecimal
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StampRevertPlannerTest {

    @Mock
    private lateinit var stampRepository: StampRepository

    @Mock
    private lateinit var stampCouponUsageRepository: StampCouponUsageRepository

    @Mock
    private lateinit var issuedCouponRepository: IssuedCouponRepository

    @InjectMocks
    private lateinit var stampRevertPlanner: StampRevertPlanner

    @Test
    @DisplayName("주문 취소 시 소진된(USED) 스탬프가 존재하면 연관된 리워드 쿠폰을 역추적하여 StampRevertPlan을 수립한다")
    fun plan_WithUsedStampsAndRewardCoupons() {
        // given
        val principal = Principal.user("U100")
        val order = Order("ORDER-100", "아메리카노 1잔", principal, 1L, BigDecimal.TEN, OrderState.PAID, emptyList())

        val usedStamp = Stamp(1L, "ORDER-100", principal, StampState.USED, LocalDateTime.now(), LocalDateTime.now().plusDays(30))

        given(stampRepository.findByOrderKey("ORDER-100"))
            .willReturn(listOf(usedStamp))

        val usage = StampCouponUsage.create(1L, 500L, LocalDateTime.now())
        given(stampCouponUsageRepository.findAllByStampIdIn(any()))
            .willReturn(listOf(usage))

        val rewardCoupon = IssuedCoupon(500L, principal, IssuedCouponState.DOWNLOADED, Coupon.rewardCoupon())
        given(issuedCouponRepository.findAllByIdIn(any()))
            .willReturn(listOf(rewardCoupon))

        // when
        val plan = stampRevertPlanner.plan(order)

        // then
        assertThat(plan.isEmpty).isFalse()
        assertThat(plan.cancelStampCount()).isEqualTo(1)
        assertThat(plan.hasRewardCouponsToCancel()).isTrue()
        assertThat(plan.rewardCouponsToCancel).containsExactly(rewardCoupon)
    }
}
