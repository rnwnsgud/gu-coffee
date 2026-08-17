package com.coffee.gu.stamp;

import com.coffee.gu.*;

import com.coffee.gu.order.Order;
import com.coffee.gu.order.OrderLine;
import com.coffee.gu.store.Store;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


@Component
public class StampHandler {
    private final StampRewardManager stampRewardManager;
    private final StampRevertManager stampRevertManager;
    private final ApplicationEventPublisher eventPublisher;

    public StampHandler(StampRewardManager stampRewardManager, StampRevertManager stampRevertManager, ApplicationEventPublisher eventPublisher) {
        this.stampRewardManager = stampRewardManager;
        this.stampRevertManager = stampRevertManager;
        this.eventPublisher = eventPublisher;
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
            eventPublisher.publishEvent(new StampEarnEvent(principal, storeId, store.getName()));
        }
    }

    public void stampToCouponWithIdempotency(StampEarnEvent event) {
        stampRewardManager.stampToCouponWithIdempotency(event);
    }

    public void revert(Order order) {
        stampRevertManager.revert(order);
    }

}
