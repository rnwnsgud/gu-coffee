package com.coffee.gu.cart

import com.coffee.gu.BaseEntity
import com.coffee.gu.Principal
import com.coffee.gu.enums.PrincipalType
import com.coffee.gu.menu.Menu
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Table(name = "cart_item")
@Entity
class CartItemEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val principalKey: String,
    @Enumerated(EnumType.STRING)
    val principalType: PrincipalType,
    val menuId: Long,
    var quantity: Long,
) : BaseEntity() {

    fun toModel(menu: Menu): CartItem {
        return CartItem(
            id = id,
            menu = menu,
            quantity = quantity,
            isDeleted = isDeleted,
        )
    }

    companion object {
        fun of(cartItem: CartItem, principal: Principal): CartItemEntity {
            return CartItemEntity(
                id = cartItem.id,
                principalKey = principal.key,
                principalType = principal.type,
                menuId = cartItem.menu.id,
                quantity = cartItem.quantity,
            ).apply {
                if (cartItem.isDeleted) {
                    delete()
                } else {
                    active()
                }
            }
        }
    }
}
