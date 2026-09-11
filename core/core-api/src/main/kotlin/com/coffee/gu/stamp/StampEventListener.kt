package com.coffee.gu.stamp

import com.coffee.gu.StampEarnEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class StampEventListener(
    private val stampHandler: StampHandler,
) {
    @Async("stampAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleStampEarned(event: StampEarnEvent) {
        stampHandler.stampToCouponWithIdempotency(event)
    }
}
