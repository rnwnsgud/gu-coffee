package com.coffee.gu.stamp;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.StampHistoryType;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

public class StampHistory {
    private Long id;
    private Principal principal;
    private StampHistoryType type;
    private Long storeId;
    private String storeName;
    private long quantity;
    private LocalDateTime recordedAt;
    @Nullable // 적립시,만료시 : 스탬프만료일, 사용시 : null,
    private LocalDateTime expiredAt;

    public StampHistory(Long id, Principal principal, StampHistoryType type, Long storeId, String storeName, long quantity, LocalDateTime recordedAt, LocalDateTime expiredAt) {
        this.id = id;
        this.principal = principal;
        this.type = type;
        this.storeId = storeId;
        this.storeName = storeName;
        this.quantity = quantity;
        this.recordedAt = recordedAt;
        this.expiredAt = expiredAt;
    }

    public static StampHistory createEarnHistory(Principal principal, Long storeId, String storeName, long quantity, LocalDateTime now, LocalDateTime expiredAt) {
        return new StampHistory(null, principal, StampHistoryType.EARNED, storeId, storeName, quantity, now, expiredAt);
    }

    public static StampHistory createUseHistory(Principal principal, Long storeId, String storeName, long quantity) {
        return new StampHistory(null, principal, StampHistoryType.USED, storeId, storeName, quantity, LocalDateTime.now(), null);
    }

    public static StampHistory createCancelHistory(Principal principal, Long storeId, String storeName, long quantity) {
        return new StampHistory(null, principal, StampHistoryType.CANCELED, storeId, storeName, quantity, LocalDateTime.now(), null);
    }

    public Long getId() {
        return id;
    }

    public StampHistoryType getType() {
        return type;
    }

    public String getStoreName() {
        return storeName;
    }

    public long getQuantity() {
        return quantity;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public @Nullable LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public Long getStoreId() {
        return storeId;
    }
}
