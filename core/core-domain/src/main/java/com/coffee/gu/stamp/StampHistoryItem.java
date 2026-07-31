package com.coffee.gu.stamp;

import com.coffee.gu.enums.StampHistoryType;
import com.coffee.gu.enums.StampState;

import java.time.LocalDateTime;

public record StampHistoryItem(
        StampHistoryType type,
        LocalDateTime displayDate,
        long quantity,
        String storeName,
        LocalDateTime expiredAt
) {

    public static StampHistoryItem from(StampHistory stampHistory) {
        return new StampHistoryItem(
                stampHistory.getType(),
                stampHistory.getRecordedAt(),
                stampHistory.getQuantity(),
                stampHistory.getStoreName(),
                stampHistory.getExpiredAt()
        );
    }
}
