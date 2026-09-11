package com.coffee.gu.stamp

import com.coffee.gu.Principal
import com.coffee.gu.StampEarnEvent
import com.coffee.gu.order.Order
import com.coffee.gu.store.Store
import com.coffee.gu.store.StoreRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class StampRecorder(
    private val stampHistoryRepository: StampHistoryRepository,
    private val storeRepository: StoreRepository,
    private val stampCouponUsageRepository: StampCouponUsageRepository,
) {
    fun recordEarn(principal: Principal, storeId: Long, stampQuantity: Int, now: LocalDateTime, expiredAt: LocalDateTime): Store {
        val store = storeRepository.findById(storeId)
        stampHistoryRepository.save(StampHistory.createEarnHistory(principal, storeId, store.name, stampQuantity, now, expiredAt))
        return store
    }

    fun recordUse(event: StampEarnEvent, stampUseCount: Int) {
        stampHistoryRepository.save(
            StampHistory.createUseHistory(
                event.principal,
                event.storeId,
                event.storeName,
                stampUseCount
            )
        )
    }

    fun recordCancel(order: Order, plan: StampRevertPlan) {
        val store = storeRepository.findById(order.storeId)
        stampHistoryRepository.save(
            StampHistory.createCancelHistory(
                order.principal,
                store.id,
                store.name,
                plan.cancelStampCount()
            )
        )
    }

    fun recordStampCouponUsages(plan: StampRewardPlan) {
        val usages = plan.createStampCouponUsages(
            plan.issuedCoupons,
            plan.stampsToUse
        )
        stampCouponUsageRepository.saveAll(usages)
    }
}
