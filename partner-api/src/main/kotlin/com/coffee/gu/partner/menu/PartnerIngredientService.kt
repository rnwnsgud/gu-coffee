package com.coffee.gu.partner.menu

import com.coffee.gu.menu.Ingredient
import com.coffee.gu.partner.auth.PartnerPrincipal
import org.springframework.stereotype.Service

@Service
class PartnerIngredientService(
    private val ingredientFinder: IngredientFinder,
    private val ingredientManager: IngredientManager,
) {
    fun getStoreIngredients(partnerPrincipal: PartnerPrincipal, storeId: Long): List<Ingredient> {
        partnerPrincipal.validateStoreOwnership(storeId)
        return ingredientFinder.findAllByStoreId(storeId)
    }

    fun updateAvailability(
        partnerPrincipal: PartnerPrincipal,
        storeId: Long,
        ingredientId: Long,
        isAvailable: Boolean,
    ): Ingredient {
        partnerPrincipal.validateStoreOwnership(storeId)
        return ingredientManager.updateAvailability(storeId, ingredientId, isAvailable)
    }
}
