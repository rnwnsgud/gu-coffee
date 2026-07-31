package com.coffee.gu.support.event;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.PrincipalType;
import com.coffee.gu.StampEarnEvent;
import com.coffee.gu.TestApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestApplication.class)
@Import({TransactionalEventThreadTest.TestEventListener.class, AsyncTestEventListener.class, TransactionalEventThreadTest.TransactionRunner.class})
@ActiveProfiles("local")
public class TransactionalEventThreadTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionalEventThreadTest.class);

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TestEventListener testEventListener;

    @Component
    public static class TestEventListener {
        private String lastThreadName;
        private String lastTransactionName;
        private boolean lastTransactionActive;
        private final CountDownLatch latch = new CountDownLatch(1);

        @EventListener
        public void handleSync(SyncTestEvent event) {
            this.lastThreadName = Thread.currentThread().getName();
            this.lastTransactionName = TransactionSynchronizationManager.getCurrentTransactionName();
            this.lastTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("[DEBUG_LOG] Sync Listener - Thread: {}, Transaction: {}, Active: {}", 
                    lastThreadName, lastTransactionName, lastTransactionActive);
        }

        public void resetLatch() {
            // No-op for sync, but for async we need to wait
        }

        public String getLastThreadName() { return lastThreadName; }
        public boolean isLastTransactionActive() { return lastTransactionActive; }
    }

    public static class SyncTestEvent {
        private final String message;
        public SyncTestEvent(String message) { this.message = message; }
    }

    @Autowired
    private TransactionRunner transactionRunner;

    @Component
    public static class TransactionRunner {
        private final ApplicationEventPublisher eventPublisher;

        public TransactionRunner(ApplicationEventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
        }

        @Transactional
        public void run(Object event) {
            eventPublisher.publishEvent(event);
        }
    }

    @Test
    @DisplayName("동기 리스너는 발행자와 동일한 스레드와 트랜잭션을 공유한다")
    void syncEventListenerTest() {
        // given
        String currentThreadName = Thread.currentThread().getName();
        log.info("[DEBUG_LOG] Publisher Thread: {}", currentThreadName);

        // when
        transactionRunner.run(new SyncTestEvent("test"));

        // then
        assertThat(testEventListener.getLastThreadName()).isEqualTo(currentThreadName);
        assertThat(testEventListener.isLastTransactionActive()).isTrue();
    }

    @Autowired
    private AsyncTestEventListener asyncTestEventListener;

    @Test
    @DisplayName("Async 리스너는 별도의 스레드에서 동작하며 트랜잭션이 분리된다")
    void asyncEventListenerTest() throws InterruptedException {
        // given
        String currentThreadName = Thread.currentThread().getName();
        log.info("[DEBUG_LOG] Publisher Thread: {}", currentThreadName);

        // when
        transactionRunner.run(new StampEarnEvent(new Principal("U1", PrincipalType.USER), 1L, "부평점"));
        
        // then
        boolean completed = asyncTestEventListener.getLatch().await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        log.info("[DEBUG_LOG] Main Thread: {}, Async Listener Thread: {}", 
                currentThreadName, asyncTestEventListener.getLastThreadName());
        
        assertThat(asyncTestEventListener.getLastThreadName()).isNotEqualTo(currentThreadName);
        assertThat(asyncTestEventListener.getLastThreadName()).startsWith("StampAsync-");
    }
}
