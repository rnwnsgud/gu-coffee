package com.coffee.gu.menu

import com.coffee.gu.enums.MenuType
import java.math.BigDecimal

class Menu(
    val id: Long = 0,
    val name: String,
    val type: MenuType,
    val price: Price,
    val imageUrl: String? = null,
    val description: String? = null,
    val detail: MenuDetail? = null,
) {
    val isStampEligible: Boolean
        get() = type == MenuType.DRINK

    val salesPrice: BigDecimal
        get() = price.salesPrice

    val costPrice: BigDecimal
        get() = price.costPrice

}
