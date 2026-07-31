package com.coffee.gu.api.controller.v1.request;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.Principal;
import com.coffee.gu.order.NewOrder;
import com.coffee.gu.order.NewOrderLine;


import java.util.List;

public record CreateOrderRequest(
        Long menuId,
        Long quantity,
        Long storeId
) {
    public NewOrder toNewOrder(Principal principal) {
        if (quantity <= 0) throw new CoreException(ErrorType.INVALID_REQUEST, null);
        return new NewOrder(principal, storeId, List.of(new NewOrderLine(menuId, quantity)));
    }
}
