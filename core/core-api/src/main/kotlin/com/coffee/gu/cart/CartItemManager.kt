package com.coffee.gu.cart

import com.coffee.gu.Principal
import com.coffee.gu.menu.MenuRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component

@Component
class CartItemManager(
    private val cartItemRepository: CartItemRepository,
    private val menuRepository: MenuRepository,
) {
    @Transactional
    fun addCartItem(principal: Principal, addCartItem: AddCartItem, cartItem: CartItem?): Long {
        val targetItem = cartItem?.apply {
            if (isDeleted) {
                active()
                applyQuantity(addCartItem.quantity)
            } else {
                applyQuantity(quantity + addCartItem.quantity)
            }
        } ?: run {
            val menu = menuRepository.findById(addCartItem.menuId)
            CartItem(menu=menu, quantity=addCartItem.quantity)
        }
        return cartItemRepository.save(targetItem, principal).id
    }

    fun modifyCartItem(cartItem: CartItem, quantity: Long, principal: Principal) {
        cartItem.applyQuantity(quantity)
        cartItemRepository.save(cartItem, principal)
    }

    @Transactional
    fun deleteCartItem(cartItem: CartItem, principal: Principal) {
        cartItem.delete()
        cartItemRepository.save(cartItem, principal)
    }
}
