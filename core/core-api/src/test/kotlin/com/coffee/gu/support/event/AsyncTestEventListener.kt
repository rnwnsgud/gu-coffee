package com.coffee.gu.support.event

import com.coffee.gu.StampEarnEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.CountDownLatch

@Component
class AsyncTestEventListener {
    private val log = LoggerFactory.getLogger(AsyncTestEventListener::class.java)

    var lastThreadName: String? = null
    var isLastTransactionActive: Boolean = false
    var latch: CountDownLatch = CountDownLatch(1)

    @Async("stampAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleStampEarned(event: StampEarnEvent) {
        this.lastThreadName = Thread.currentThread().name
        this.isLastTransactionActive = TransactionSynchronizationManager.isActualTransactionActive()

        log.info(
            "[DEBUG_LOG] Async Listener - Thread: {}, Transaction Active: {}",
            lastThreadName, isLastTransactionActive
        )

        latch.countDown()
    }

    fun resetLatch() {
        this.latch = CountDownLatch(1)
    }
}
