package com.coffee.gu.api.controller.v1.response.menu

import com.coffee.gu.menu.Menu
import java.math.BigDecimal

class MenuResponse(
    val id: Long,
    val name: String,
    val salesPrice: BigDecimal,
) {
    companion object {
        fun from(menus: List<Menu>): List<MenuResponse> {
            return menus.map { menu ->
                MenuResponse(
                    id = menu.id,
                    name = menu.name,
                    salesPrice = menu.salesPrice
                )
            }
        }
    }
}
