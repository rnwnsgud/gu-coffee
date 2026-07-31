package com.coffee.gu.api.controller.v1.request;

import com.coffee.gu.cart.ModifyCartItem;

public record ModifyCartItemRequest(
        Long quantity
) {
    public ModifyCartItem toModifyCartItem(Long cartItemId) {
        return new ModifyCartItem(cartItemId, quantity);
    }
}
