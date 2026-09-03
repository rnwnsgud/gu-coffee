package com.coffee.gu.cart

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.menu.Menu

class CartItem(
    val id: Long? = null,
    val menu: Menu,
    var quantity: Long,
    var isDeleted: Boolean = false,
) {
    fun resolveMenu(completedMenu: Menu): CartItem {
        if (this.menu.id != completedMenu.id) {
            throw CoreException(ErrorType.SYSTEM_LOGIC_ERROR, "장바구니의 메뉴 ID와 조립하려는 메뉴 ID가 일치하지 않습니다.")
        }
        return CartItem(this.id, completedMenu, this.quantity, this.isDeleted)
    }

    fun applyQuantity(quantity: Long) {
        this.quantity = if (quantity < 1) 1 else quantity
    }

    fun delete() {
        this.isDeleted = true
    }

    fun active() {
        this.isDeleted = false
    }

    companion object {
        @JvmStatic
        fun create(menuId: Long, quantity: Long): CartItem {
            return CartItem(id = null, menu = Menu.createIdOnly(menuId), quantity = quantity, isDeleted = false)
        }

        @JvmStatic
        fun createUnresolved(id: Long, menuId: Long, quantity: Long, isDeleted: Boolean): CartItem {
            return CartItem(id = id, menu = Menu.createIdOnly(menuId), quantity = quantity, isDeleted = isDeleted)
        }
    }
}
