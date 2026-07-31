package com.coffee.gu.stamp;

import com.coffee.gu.StampEarnEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class StampEventListener {

    private final StampHandler stampHandler;

    public StampEventListener(StampHandler stampHandler) {
        this.stampHandler = stampHandler;
    }

    @Async("stampAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStampEarned(StampEarnEvent event){
        stampHandler.stampToCouponWithIdempotency(event);
    }

}
