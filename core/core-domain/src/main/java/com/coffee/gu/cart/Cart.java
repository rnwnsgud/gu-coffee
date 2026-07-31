package com.coffee.gu.cart;


import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.Principal;
import com.coffee.gu.order.NewOrder;
import com.coffee.gu.order.NewOrderLine;

import java.util.List;
import java.util.Set;

public class Cart {
    private Principal principal;
    private List<CartItem> items;

    public Cart(Principal principal, List<CartItem> items) {
        this.principal = principal;
        this.items = items;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public NewOrder toNewOrder(Set<Long> targetItemIds, Long storeId) {
        if (items.isEmpty()) throw new CoreException(ErrorType.INVALID_REQUEST, null);
        List<NewOrderLine> lines = items.stream()
                .filter(item -> targetItemIds.contains(item.getId()))
                .map(item -> new NewOrderLine(
                        item.getMenu().getId(),
                        item.getQuantity()
                ))
                .toList();

        return new NewOrder(
                principal,
                storeId,
                lines
        );
    }
}
