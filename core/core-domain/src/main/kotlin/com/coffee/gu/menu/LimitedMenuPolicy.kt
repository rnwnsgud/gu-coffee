package com.coffee.gu.menu

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType

data class LimitedMenuPolicy(
    val menuId: Long,
    val maxPurchasePerUser: Int,
) {
    fun validateOrderQuantity(requestedQuantity: Int) {
        if (requestedQuantity > maxPurchasePerUser) {
            throw CoreException(ErrorType.LIMITED_MENU_EXCEED_MAX_PURCHASE_QUANTITY)
        }
    }
}
