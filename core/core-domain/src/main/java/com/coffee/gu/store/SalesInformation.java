package com.coffee.gu.store;

import java.util.List;

public record SalesInformation(
        StoreLocation location,
        List<SalesHour> hours,
        String phoneNumber
) {
}
