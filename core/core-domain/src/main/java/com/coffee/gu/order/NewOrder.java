package com.coffee.gu.order;


import com.coffee.gu.Principal;

import java.util.List;

public record NewOrder(
        Principal principal,
        Long storeId,
        List<NewOrderLine> lines
) {
}
