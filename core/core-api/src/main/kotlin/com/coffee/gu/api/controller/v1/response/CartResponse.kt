package com.coffee.gu.api.controller.v1.response

import com.coffee.gu.cart.CartItem
import java.math.BigDecimal

class CartResponse(
    val items: List<CartItemResponse>
) {
    class CartItemResponse(
        val id: Long,
        val menuId: Long?,
        val menuName: String?,
        val imageUrl: String?,
        val description: String?,
        val costPrice: BigDecimal?,
        val salesPrice: BigDecimal?,
        val quantity: Long,
    ) {
        companion object {
            @JvmStatic
            fun from(cartItem: CartItem): CartItemResponse {
                val menu = cartItem.menu
                return CartItemResponse(
                    id = cartItem.id,
                    menuId = menu?.id,
                    menuName = menu?.name,
                    imageUrl = menu?.imageUrl,
                    description = menu?.description,
                    costPrice = menu?.costPrice,
                    salesPrice = menu?.salesPrice,
                    quantity = cartItem.quantity
                )
            }
        }
    }
}
