package com.coffee.gu.partner.controller.v1.response

import com.coffee.gu.menu.Ingredient

data class PartnerIngredientResponse(
    val storeId: Long,
    val ingredientId: Long,
    val name: String,
    val isAvailable: Boolean,
) {
    companion object {
        fun from(ingredient: Ingredient): PartnerIngredientResponse {
            return PartnerIngredientResponse(
                storeId = ingredient.storeId,
                ingredientId = ingredient.ingredientId,
                name = ingredient.name,
                isAvailable = ingredient.isAvailable,
            )
        }
    }
}
