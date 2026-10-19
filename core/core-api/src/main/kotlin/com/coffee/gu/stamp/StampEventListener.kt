package com.coffee.gu.stamp

import com.coffee.gu.StampEarnEvent
import com.coffee.gu.stamp.messaging.StampMessagingProducer
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class StampEventListener(
    private val stampMessagingProducer: StampMessagingProducer,
) {
    @Async("stampAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun handleStampEarned(event: StampEarnEvent) {
        stampMessagingProducer.send(event)
    }
}
