package com.coffee.gu.api.controller.v1.request;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.cart.AddCartItem;


public record AddCartItemRequest(
        Long menuId,
        Long quantity
) {
    public AddCartItem toAddCartItem() {
        if (quantity <= 0) throw new CoreException(ErrorType.INVALID_REQUEST, null);
        return new AddCartItem(menuId, quantity);
    }
}
