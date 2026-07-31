package com.coffee.gu.api.controller.v1.response;

import com.coffee.gu.cart.CartItem;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        List<CartItemResponse> items
) {


    public record CartItemResponse(
            Long id,
            Long menuId,
            String menuName,
            String imageUrl,
            String description,
            BigDecimal costPrice,
            BigDecimal salesPrice,
            Long quantity) {
        public static CartItemResponse from(CartItem cartItem) {
            return new CartItemResponse(
                    cartItem.getId(),
                    cartItem.getMenu().getId(),
                    cartItem.getMenu().getName(),
                    cartItem.getMenu().getImageUrl(),
                    cartItem.getMenu().getDescription(),
                    cartItem.getMenu().getCostPrice(),
                    cartItem.getMenu().getSalesPrice(),
                    cartItem.getQuantity()
            );
        }
    }
}

