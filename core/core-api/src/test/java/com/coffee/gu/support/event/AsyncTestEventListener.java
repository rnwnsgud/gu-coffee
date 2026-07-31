package com.coffee.gu.support.event;

import com.coffee.gu.StampEarnEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CountDownLatch;

@Component
public class AsyncTestEventListener {
    private static final Logger log = LoggerFactory.getLogger(AsyncTestEventListener.class);
    
    private String lastThreadName;
    private boolean lastTransactionActive;
    private CountDownLatch latch = new CountDownLatch(1);

    @Async("stampAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStampEarned(StampEarnEvent event) {
        this.lastThreadName = Thread.currentThread().getName();
        this.lastTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
        
        log.info("[DEBUG_LOG] Async Listener - Thread: {}, Transaction Active: {}", 
                lastThreadName, lastTransactionActive);
        
        latch.countDown();
    }

    public String getLastThreadName() {
        return lastThreadName;
    }

    public boolean isLastTransactionActive() {
        return lastTransactionActive;
    }

    public CountDownLatch getLatch() {
        return latch;
    }
    
    public void resetLatch() {
        this.latch = new CountDownLatch(1);
    }
}
