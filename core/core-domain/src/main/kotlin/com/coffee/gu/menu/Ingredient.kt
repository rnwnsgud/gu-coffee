package com.coffee.gu.menu

class Ingredient(
    val id: Long = 0,
    val storeId: Long,
    val ingredientId: Long,
    val name: String,
    var isAvailable: Boolean = true,
) {
    fun updateAvailability(isAvailable: Boolean) {
        this.isAvailable = isAvailable
    }
}
