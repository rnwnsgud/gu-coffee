package com.coffee.gu.menu

import com.coffee.gu.enums.MenuType
import java.math.BigDecimal

class Menu(
    val id: Long = 0,
    val name: String? = null,
    val type: MenuType? = null,
    val price: Price? = null,
    val imageUrl: String? = null,
    val description: String? = null,
    val detail: MenuDetail? = null,
) {
    val isStampEligible: Boolean
        get() = type == MenuType.DRINK

    val salesPrice: BigDecimal?
        get() = price?.salesPrice

    val costPrice: BigDecimal?
        get() = price?.costPrice

    companion object {
        @JvmStatic
        fun createIdOnly(id: Long): Menu = Menu(id = id)
    }
}
