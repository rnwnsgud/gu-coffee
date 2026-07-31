package com.coffee.gu.stamp;

import com.coffee.gu.*;

import com.coffee.gu.order.Order;
import com.coffee.gu.store.Store;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Component
public class StampHandler {
    private final StampRewardManager stampRewardManager;
    private final StampRevertManager stampRevertManager;
    private final StampRecorder stampRecorder;
    private final ApplicationEventPublisher eventPublisher;

    public StampHandler(StampRewardManager stampRewardManager, StampRevertManager stampRevertManager, ApplicationEventPublisher eventPublisher, StampRecorder stampRecorder) {
        this.stampRewardManager = stampRewardManager;
        this.stampRevertManager = stampRevertManager;
        this.eventPublisher = eventPublisher;
        this.stampRecorder = stampRecorder;
    }

    @Transactional
    public void reward(Principal principal, String orderKey, Long storeId, Long stampQuantity) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.plusDays(Stamp.EXPIRY_DAYS);
        stampRewardManager.reward(principal, orderKey, stampQuantity, expiredAt);
        Store store = stampRecorder.recordEarn(principal, storeId, stampQuantity, now, expiredAt);
        eventPublisher.publishEvent(new StampEarnEvent(principal, storeId, store.getName()));
    }

    public void stampToCouponWithIdempotency(StampEarnEvent event) {
        stampRewardManager.stampToCouponWithIdempotency(event);
    }

    public void revert(Order order) {
        stampRevertManager.revert(order);
    }

    public void validateRevertable(Order order) {
        stampRevertManager.validateRevertable(order);
    }


}
