package com.coffee.gu.menu

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType

class LimitedMenu(
    val id: Long = 0,
    val menu: Menu,
    val totalQuantity: Int,
    val soldQuantity: Int = 0,
    val maxPurchasePerUser: Int = 1,
) {
    val isSoldOut: Boolean
        get() = soldQuantity >= totalQuantity

    val remainingQuantity: Int
        get() = (totalQuantity - soldQuantity).coerceAtLeast(0)

    fun increaseSoldQuantity(quantity: Int = 1): LimitedMenu {
        if (quantity <= 0) {
            throw CoreException(ErrorType.INVALID_REQUEST)
        }
        if (soldQuantity + quantity > totalQuantity) {
            throw CoreException(ErrorType.LIMITED_MENU_SOLD_OUT)
        }
        return LimitedMenu(
            id = this.id,
            menu = this.menu,
            totalQuantity = this.totalQuantity,
            soldQuantity = this.soldQuantity + quantity,
            maxPurchasePerUser = this.maxPurchasePerUser,
        )
    }

    fun decreaseSoldQuantity(quantity: Int = 1): LimitedMenu {
        if (quantity <= 0) {
            throw CoreException(ErrorType.INVALID_REQUEST)
        }
        return LimitedMenu(
            id = this.id,
            menu = this.menu,
            totalQuantity = this.totalQuantity,
            soldQuantity = (this.soldQuantity - quantity).coerceAtLeast(0),
            maxPurchasePerUser = this.maxPurchasePerUser,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LimitedMenu) return false
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
