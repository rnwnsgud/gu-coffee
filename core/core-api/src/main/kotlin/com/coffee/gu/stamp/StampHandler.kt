package com.coffee.gu.stamp

import com.coffee.gu.PaymentApprovedEvent
import com.coffee.gu.StampEarnEvent
import com.coffee.gu.order.Order
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class StampHandler(
    private val stampRewardManager: StampRewardManager,
    private val stampRevertManager: StampRevertManager,
) {
    fun reward(order: Order) {
        val stampEligibleQuantity = order.lines
            .filter { it.isStampEligible }
            .sumOf { it.quantity }
            .toInt()

        if (stampEligibleQuantity > 0) {
            val now = LocalDateTime.now()
            val expiredAt = now.plusDays(Stamp.EXPIRY_DAYS)
            val principal = order.principal
            val orderKey = order.key
            val storeId = order.storeId
            stampRewardManager.reward(principal, orderKey, stampEligibleQuantity, expiredAt, now, storeId)
        }
    }

    fun completePaymentApprovedEvent(event: PaymentApprovedEvent) {
        stampRewardManager.completePaymentApprovedEvent(event)
    }

    fun stampToCouponWithIdempotency(event: StampEarnEvent) {
        stampRewardManager.stampToCouponWithIdempotency(event)
    }

    fun revert(order: Order) {
        stampRevertManager.revert(order)
    }
}
