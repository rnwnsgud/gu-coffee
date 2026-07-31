package com.coffee.gu.store;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record SalesHour(
        Long storeId,
        DayOfWeek day,
        LocalTime open,
        LocalTime close
) {
}
