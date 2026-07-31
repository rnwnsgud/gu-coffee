package com.coffee.gu.stamp;


import com.coffee.gu.Principal;
import com.coffee.gu.enums.PrincipalType;
import com.coffee.gu.enums.StampHistoryType;
import com.coffee.gu.enums.StampState;
import com.coffee.gu.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "stamp_history")
public class StampHistoryEntity extends BaseEntity {
    private String principalKey;
    @Enumerated(EnumType.STRING)
    private PrincipalType principalType;
    @Enumerated(EnumType.STRING)
    private StampHistoryType type;
    private Long storeId;
    private String storeName;
    private long quantity;
    private LocalDateTime recordedAt;
    private LocalDateTime expiredAt;

    protected StampHistoryEntity() {
    }

    public StampHistoryEntity(String principalKey, PrincipalType principalType, StampHistoryType type, Long storeId, String storeName, long quantity, LocalDateTime recordedAt, LocalDateTime expiredAt) {
        this.principalKey = principalKey;
        this.principalType = principalType;
        this.type = type;
        this.storeId = storeId;
        this.storeName = storeName;
        this.quantity = quantity;
        this.recordedAt = recordedAt;
        this.expiredAt = expiredAt;
    }

    public static StampHistoryEntity from(StampHistory stampHistory) {
        return new StampHistoryEntity(
                stampHistory.getPrincipal().getKey(),
                stampHistory.getPrincipal().getType(),
                stampHistory.getType(),
                stampHistory.getStoreId(),
                stampHistory.getStoreName(),
                stampHistory.getQuantity(),
                stampHistory.getRecordedAt(),
                stampHistory.getExpiredAt());
    }

    public String getPrincipalKey() {
        return principalKey;
    }

    public StampHistoryType getType() {
        return type;
    }

    public Long getStoreId() {
        return storeId;
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

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public StampHistory toModel() {
        return new StampHistory(
                this.id,
                new Principal(principalKey, principalType),
                this.type,
                this.storeId,
                this.storeName,
                this.quantity,
                this.recordedAt,
                this.expiredAt
        );
    }
}
