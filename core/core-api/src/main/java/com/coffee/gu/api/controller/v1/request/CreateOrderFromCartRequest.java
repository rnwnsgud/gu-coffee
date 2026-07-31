package com.coffee.gu.api.controller.v1.request;

import java.util.Set;

public record CreateOrderFromCartRequest(
        Long storeId,
        Set<Long> cartItemIds
) {
}
