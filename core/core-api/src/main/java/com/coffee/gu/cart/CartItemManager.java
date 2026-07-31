package com.coffee.gu.cart;


import com.coffee.gu.Principal;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
public class CartItemManager {

    private final CartItemRepository cartItemRepository;

    public CartItemManager(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    @Transactional
    public Long addCartItem(Principal principal, AddCartItem addCartItem, Optional<CartItem> cartItem) {
        return cartItem.map(item -> {
            if (item.getIsDeleted()) {
                item.active();
                item.applyQuantity(addCartItem.quantity());
            } else {
                item.applyQuantity(item.getQuantity() + addCartItem.quantity());
            }
            return cartItemRepository.save(item, principal).getId();
        }).orElseGet(() -> cartItemRepository.save(CartItem.create(addCartItem.menuId(), addCartItem.quantity()), principal).getId());
    }

    public void modifyCartItem(CartItem cartItem, Long quantity, Principal principal) {
        cartItem.applyQuantity(quantity);
        cartItemRepository.save(cartItem, principal);
    }

    @Transactional
    public void deleteCartItem(CartItem cartItem, Principal principal) {
        cartItem.delete();
        cartItemRepository.save(cartItem, principal);
    }
}
