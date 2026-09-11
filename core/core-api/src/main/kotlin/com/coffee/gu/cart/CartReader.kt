package com.coffee.gu.cart

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.menu.MenuFinder
import org.springframework.stereotype.Component

@Component
class CartReader(
    private val cartItemRepository: CartItemRepository,
    private val menuFinder: MenuFinder
) {
    fun findByPrincipal(principal: Principal): List<CartItem> {
        val cartItems = cartItemRepository.findByPrincipalKey(principal.key)
        if (cartItems.isEmpty()) return emptyList()

        val menuIds = cartItems.map { it.menu.id }
        val menuMap = menuFinder.findAllByIdIn(menuIds).associateBy { it.id }

        return cartItems.mapNotNull { cartItem ->
            val menu = menuMap[cartItem.menu.id] ?: return@mapNotNull null
            CartItem(cartItem.id, menu, cartItem.quantity, cartItem.isDeleted)
        }
    }

    fun findByPrincipalAndMenuId(principal: Principal, menuId: Long): CartItem? {
        return cartItemRepository.findByPrincipalKeyAndMenuIdIncludingDeleted(principal.key, menuId)
    }

    fun getByPrincipalAndId(principal: Principal, cartItemId: Long): CartItem {
        val cartItems = findByPrincipal(principal)
        return cartItems.firstOrNull { it.id == cartItemId }
            ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
    }
}
