package com.coffee.gu.cart

import com.coffee.gu.Principal
import java.util.Optional

interface CartItemRepository {
    fun findByPrincipalKey(principalKey: String): List<CartItem>
    fun findByPrincipalIncludingDeleted(principalKey: String): Optional<CartItem>
    fun save(cartItem: CartItem, principal: Principal): CartItem
}
