package com.coffee.gu.cart;

import com.coffee.gu.Principal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private final CartReader cartReader;
    private final CartItemManager cartItemManager;

    public CartService(CartReader cartReader, CartItemManager cartItemManager) {
        this.cartReader = cartReader;
        this.cartItemManager = cartItemManager;
    }

    public Cart getCart(Principal principal) {
        List<CartItem> cartItems = cartReader.findByPrincipal(principal);
        return new Cart(
                principal,
                cartItems.stream().map(cartItem -> new CartItem(
                        cartItem.getId(),
                        cartItem.getMenu(),
                        cartItem.getQuantity(),
                        cartItem.getIsDeleted()
                )).toList()
        );
    }

    public Long addCartItem(Principal principal, AddCartItem addCartItem) {
        Optional<CartItem> cartItem = cartReader.findByPrincipalAndMenuId(principal, addCartItem.menuId());
        return cartItemManager.addCartItem(principal, addCartItem, cartItem);
    }

    public Long modifyCartItem(Principal principal, ModifyCartItem modifyCartItem) {
        CartItem cartItem = cartReader.getByPrincipalAndId(principal, modifyCartItem.cartItemId());
        cartItemManager.modifyCartItem(cartItem, modifyCartItem.quantity(), principal);
        return cartItem.getId();
    }

    public void deleteCartItem(Principal principal, Long cartItemId) {
        CartItem cartItem = cartReader.getByPrincipalAndId(principal, cartItemId);
        cartItemManager.deleteCartItem(cartItem, principal);
    }
}
