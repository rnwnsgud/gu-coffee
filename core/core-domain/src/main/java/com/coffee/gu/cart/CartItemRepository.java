package com.coffee.gu.cart;


import com.coffee.gu.Principal;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository {
    List<CartItem> findByPrincipalKey(String principalKey);
    Optional<CartItem> findByPrincipalIncludingDeleted(String principalKey);

    CartItem save(CartItem cartItem, Principal principal);
}
