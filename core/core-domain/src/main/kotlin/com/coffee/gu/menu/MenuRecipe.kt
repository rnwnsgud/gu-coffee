package com.coffee.gu.menu

class MenuRecipe(
    val id: Long = 0,
    val menuId: Long,
    val ingredientId: Long,
    val requiredQuantity: Int = 1,
)
