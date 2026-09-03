package com.coffee.gu.order

import java.math.BigDecimal

class OrderLine(
    val id: Long? = null,
    val orderKey: String,
    val menuId: Long,
    val menuName: String,
    val imageUrl: String,
    val description: String,
    val quantity: Long,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal,
    val isStampEligible: Boolean,
) {
    companion object {
        @JvmStatic
        fun create(
            orderKey: String,
            menuId: Long,
            menuName: String,
            imageUrl: String,
            description: String,
            quantity: Long,
            unitPrice: BigDecimal,
            totalPrice: BigDecimal,
            isStampEligible: Boolean
        ): OrderLine = OrderLine(
            id = null,
            orderKey = orderKey,
            menuId = menuId,
            menuName = menuName,
            imageUrl = imageUrl,
            description = description,
            quantity = quantity,
            unitPrice = unitPrice,
            totalPrice = totalPrice,
            isStampEligible = isStampEligible
        )
    }
}
