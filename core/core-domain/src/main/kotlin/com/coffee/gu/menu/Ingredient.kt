package com.coffee.gu.menu

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType

class Ingredient(
    val id: Long = 0,
    val storeId: Long,
    val ingredientId: Long,
    val name: String,
    var stockQuantity: Int,
    var isAvailable: Boolean = true,
) {
    fun canConsume(quantity: Int): Boolean {
        return isAvailable && stockQuantity >= quantity
    }

    fun updateAvailability(isAvailable: Boolean) {
        this.isAvailable = isAvailable
    }

    fun updateStockQuantity(stockQuantity: Int) {
        if (stockQuantity < 0) {
            throw CoreException(ErrorType.INVALID_REQUEST)
        }
        this.stockQuantity = stockQuantity
    }
}
