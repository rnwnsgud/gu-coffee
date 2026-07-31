package com.coffee.gu.stamp;

import java.time.LocalDateTime;
import java.util.List;

public interface StampHistoryRepository {
    StampHistory save(StampHistory stampHistory);
    List<StampHistory> getWithinNMonths(String principalKey, LocalDateTime nMonthsAgo);
}
