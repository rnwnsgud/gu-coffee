package com.coffee.gu.api.controller.v1.response.menu

import com.coffee.gu.menu.Ingredient

data class PartnerIngredientResponse(
    val storeId: Long,
    val ingredientId: Long,
    val name: String,
    val stockQuantity: Int,
    val isAvailable: Boolean,
) {
    companion object {
        fun from(ingredient: Ingredient): PartnerIngredientResponse {
            return PartnerIngredientResponse(
                storeId = ingredient.storeId,
                ingredientId = ingredient.ingredientId,
                name = ingredient.name,
                stockQuantity = ingredient.stockQuantity,
                isAvailable = ingredient.isAvailable,
            )
        }
    }
}
