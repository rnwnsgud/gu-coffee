package com.coffee.gu.stamp;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.Principal;
import com.coffee.gu.StampEarnEvent;
import com.coffee.gu.order.Order;
import com.coffee.gu.store.Store;
import com.coffee.gu.store.StoreRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class StampRecorder {

    private final StampHistoryRepository stampHistoryRepository;
    private final StoreRepository storeRepository;
    private final StampCouponUsageRepository stampCouponUsageRepository;

    public StampRecorder(StampHistoryRepository stampHistoryRepository, StoreRepository storeRepository, StampCouponUsageRepository stampCouponUsageRepository) {
        this.stampHistoryRepository = stampHistoryRepository;
        this.storeRepository = storeRepository;
        this.stampCouponUsageRepository = stampCouponUsageRepository;
    }

    public Store recordEarn(Principal principal, Long storeId, Long stampQuantity, LocalDateTime now, LocalDateTime expiredAt) {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA, null));
        stampHistoryRepository.save(StampHistory.createEarnHistory(principal, storeId, store.getName(), stampQuantity, now, expiredAt));
        return store;
    }

    public void recordUse(StampEarnEvent event, long stampUseCount) {
        stampHistoryRepository.save(
                StampHistory.createUseHistory(
                        event.getPrincipal(),
                        event.getStoreId(),
                        event.getStoreName(),
                        stampUseCount
                )
        );
    }

    public void recordCancel(Order order, StampRevertPlan plan) {
        Store store = storeRepository.findById(order.getStoreId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA, null));
        stampHistoryRepository.save(
                StampHistory.createCancelHistory(
                        order.getPrincipal(),
                        store.getId(),
                        store.getName(),
                        plan.cancelStampCount()
                )
        );
    }

    public void recordStampCouponUsages(StampRewardPlan plan) {
        List<StampCouponUsage> usages = plan.createStampCouponUsages(
                plan.getIssuedCoupons(),
                plan.getStampsToUse()
        );
        stampCouponUsageRepository.saveAll(usages);
    }
}
