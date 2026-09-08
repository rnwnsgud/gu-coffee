package com.coffee.gu.cart

import com.coffee.gu.BaseEntity
import com.coffee.gu.Principal
import com.coffee.gu.enums.PrincipalType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Table(name = "cart_item")
@Entity
class CartItemEntity @JvmOverloads constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val principalKey: String,
    @Enumerated(EnumType.STRING)
    val principalType: PrincipalType,
    val menuId: Long,
    var quantity: Long,
) : BaseEntity() {

    fun toModel(): CartItem {
        return CartItem.createUnresolved(
            id = id,
            menuId = menuId,
            quantity = quantity,
            isDeleted = isDeleted,
        )
    }

    companion object {
        @JvmStatic
        fun of(cartItem: CartItem, principal: Principal): CartItemEntity {
            return CartItemEntity(
                id = cartItem.id ?: 0L,
                principalKey = principal.key,
                principalType = principal.type,
                menuId = cartItem.menu?.id ?: 0L,
                quantity = cartItem.quantity,
            )
        }
    }
}
