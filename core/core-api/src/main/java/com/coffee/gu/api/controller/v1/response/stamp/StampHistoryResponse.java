package com.coffee.gu.api.controller.v1.response.stamp;

import com.coffee.gu.stamp.StampHistory;
import com.coffee.gu.stamp.StampHistoryItem;

import java.util.List;

public record StampHistoryResponse(
        long expiringSoonCount,
        List<StampHistoryItem> histories
) {
    public static StampHistoryResponse of(long expiringSoonCount, List<StampHistory> stampHistories) {
        List<StampHistoryItem> stampHistoryItems = stampHistories.stream()
                .map(StampHistoryItem::from)
                .toList();
        return new StampHistoryResponse(expiringSoonCount, stampHistoryItems);
    }


}
