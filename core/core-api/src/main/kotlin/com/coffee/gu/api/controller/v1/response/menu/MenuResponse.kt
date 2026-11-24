package com.coffee.gu.api.controller.v1.response.menu

import com.coffee.gu.menu.MenuItemResult
import java.math.BigDecimal

class MenuResponse(
    val id: Long,
    val name: String,
    val salesPrice: BigDecimal,
    val isSoldOut: Boolean,
) {
    companion object {
        fun from(items: List<MenuItemResult>): List<MenuResponse> {
            return items.map { item ->
                MenuResponse(
                    id = item.menu.id,
                    name = item.menu.name,
                    salesPrice = item.menu.salesPrice,
                    isSoldOut = item.isSoldOut,
                )
            }
        }
    }
}
