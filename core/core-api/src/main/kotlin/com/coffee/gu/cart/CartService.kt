package com.coffee.gu.cart

import com.coffee.gu.Principal
import org.springframework.stereotype.Service

@Service
class CartService(
    private val cartReader: CartReader,
    private val cartItemManager: CartItemManager
) {
    fun getCart(principal: Principal): Cart {
        val cartItems = cartReader.findByPrincipal(principal)
        return Cart(
            principal = principal,
            items = cartItems.map { cartItem ->
                CartItem(
                    id = cartItem.id,
                    menu = cartItem.menu,
                    quantity = cartItem.quantity,
                    isDeleted = cartItem.isDeleted
                )
            }
        )
    }

    fun addCartItem(principal: Principal, addCartItem: AddCartItem): Long {
        val cartItem = cartReader.findByPrincipalAndMenuId(principal, addCartItem.menuId)
        return cartItemManager.addCartItem(principal, addCartItem, cartItem)
    }

    fun modifyCartItem(principal: Principal, modifyCartItem: ModifyCartItem): Long {
        val cartItem = cartReader.getByPrincipalAndId(principal, modifyCartItem.cartItemId)
        cartItemManager.modifyCartItem(cartItem, modifyCartItem.quantity, principal)
        return cartItem.id
    }

    fun deleteCartItem(principal: Principal, cartItemId: Long) {
        val cartItem = cartReader.getByPrincipalAndId(principal, cartItemId)
        cartItemManager.deleteCartItem(cartItem, principal)
    }
}
