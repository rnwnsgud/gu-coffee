package com.coffee.gu.support.event

import com.coffee.gu.Principal
import com.coffee.gu.StampEarnEvent
import com.coffee.gu.TestApplication
import com.coffee.gu.enums.PrincipalType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Import
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootTest(classes = [TestApplication::class])
@Import(
    TransactionalEventThreadTest.TestEventListener::class,
    AsyncTestEventListener::class,
    TransactionalEventThreadTest.TransactionRunner::class
)
@ActiveProfiles("local")
class TransactionalEventThreadTest {

    companion object {
        private val log = LoggerFactory.getLogger(TransactionalEventThreadTest::class.java)
    }

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    private lateinit var testEventListener: TestEventListener

    @Component
    class TestEventListener {
        var lastThreadName: String? = null
        var lastTransactionName: String? = null
        var isLastTransactionActive: Boolean = false
        val latch: CountDownLatch = CountDownLatch(1)

        @EventListener
        fun handleSync(event: SyncTestEvent) {
            this.lastThreadName = Thread.currentThread().name
            this.lastTransactionName = TransactionSynchronizationManager.getCurrentTransactionName()
            this.isLastTransactionActive = TransactionSynchronizationManager.isActualTransactionActive()
            log.info(
                "[DEBUG_LOG] Sync Listener - Thread: {}, Transaction: {}, Active: {}",
                lastThreadName, lastTransactionName, isLastTransactionActive
            )
        }

        fun resetLatch() {
            // No-op for sync, but for async we need to wait
        }
    }

    data class SyncTestEvent(val message: String)

    @Autowired
    private lateinit var transactionRunner: TransactionRunner

    @Component
    class TransactionRunner(private val eventPublisher: ApplicationEventPublisher) {

        @Transactional
        fun run(event: Any) {
            eventPublisher.publishEvent(event)
        }
    }

    @Test
    @DisplayName("동기 리스너는 발행자와 동일한 스레드와 트랜잭션을 공유한다")
    fun syncEventListenerTest() {
        // given
        val currentThreadName = Thread.currentThread().name
        log.info("[DEBUG_LOG] Publisher Thread: {}", currentThreadName)

        // when
        transactionRunner.run(SyncTestEvent("test"))

        // then
        assertThat(testEventListener.lastThreadName).isEqualTo(currentThreadName)
        assertThat(testEventListener.isLastTransactionActive).isTrue()
    }

    @Autowired
    private lateinit var asyncTestEventListener: AsyncTestEventListener

    @Test
    @DisplayName("Async 리스너는 별도의 스레드에서 동작하며 트랜잭션이 분리된다")
    fun asyncEventListenerTest() {
        // given
        val currentThreadName = Thread.currentThread().name
        log.info("[DEBUG_LOG] Publisher Thread: {}", currentThreadName)

        // when
        transactionRunner.run(StampEarnEvent(Principal("U1", PrincipalType.USER), 1L, "부평점"))

        // then
        val completed = asyncTestEventListener.latch.await(5, TimeUnit.SECONDS)
        assertThat(completed).isTrue()

        log.info(
            "[DEBUG_LOG] Main Thread: {}, Async Listener Thread: {}",
            currentThreadName, asyncTestEventListener.lastThreadName
        )

        assertThat(asyncTestEventListener.lastThreadName).isNotEqualTo(currentThreadName)
        assertThat(asyncTestEventListener.lastThreadName).startsWith("StampAsync-")
    }
}
