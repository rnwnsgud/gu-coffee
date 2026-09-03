package com.coffee.gu.cart

import com.coffee.gu.Principal

interface CartItemRepository {
    fun findByPrincipalKey(principalKey: String): List<CartItem>
    fun findByPrincipalIncludingDeleted(principalKey: String): CartItem?
    fun save(cartItem: CartItem, principal: Principal): CartItem
}
