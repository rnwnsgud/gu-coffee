package com.coffee.gu.api.controller.v1.response.menu

import com.coffee.gu.menu.LimitedMenu
import java.math.BigDecimal

data class LimitedMenuResponse(
    val id: Long,
    val menuId: Long,
    val menuName: String,
    val salesPrice: BigDecimal,
    val totalQuantity: Int,
    val soldQuantity: Int,
    val availableStock: Int,
    val maxPurchasePerUser: Int,
) {
    companion object {
        fun of(limitedMenu: LimitedMenu, availableStock: Int): LimitedMenuResponse {
            return LimitedMenuResponse(
                id = limitedMenu.id,
                menuId = limitedMenu.menu.id,
                menuName = limitedMenu.menu.name ?: "",
                salesPrice = limitedMenu.menu.salesPrice ?: BigDecimal.ZERO,
                totalQuantity = limitedMenu.totalQuantity,
                soldQuantity = limitedMenu.soldQuantity,
                availableStock = availableStock,
                maxPurchasePerUser = limitedMenu.maxPurchasePerUser,
            )
        }
    }
}
