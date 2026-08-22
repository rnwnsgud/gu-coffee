package com.coffee.gu.stamp;

import com.coffee.gu.*;

import com.coffee.gu.event.OutboxEventPublisher;
import com.coffee.gu.order.Order;
import com.coffee.gu.order.OrderLine;
import com.coffee.gu.store.Store;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class StampHandler {
    private final StampRewardManager stampRewardManager;
    private final StampRevertManager stampRevertManager;
    private final OutboxEventPublisher outboxEventPublisher;

    public StampHandler(StampRewardManager stampRewardManager, StampRevertManager stampRevertManager, OutboxEventPublisher outboxEventPublisher) {
        this.stampRewardManager = stampRewardManager;
        this.stampRevertManager = stampRevertManager;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    public void reward(Order order) {
        long stampEligibleQuantity = order.getLines().stream()
                .filter(OrderLine::getIsStampEligible)
                .mapToLong(OrderLine::getQuantity)
                .sum();
        if (stampEligibleQuantity > 0) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiredAt = now.plusDays(Stamp.EXPIRY_DAYS);
            Principal principal = order.getPrincipal();
            String orderKey = order.getKey();
            Long storeId = order.getStoreId();
            Store store = stampRewardManager.reward(principal, orderKey, stampEligibleQuantity, expiredAt, now, storeId);
            StampEarnEvent event = new StampEarnEvent(principal, storeId, store.getName());
            outboxEventPublisher.publishOutboxEvent(event);
        }
    }

    public void stampToCouponWithIdempotency(StampEarnEvent event) {
        stampRewardManager.stampToCouponWithIdempotency(event);
    }

    public void revert(Order order) {
        stampRevertManager.revert(order);
    }
}
