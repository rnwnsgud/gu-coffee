package com.coffee.gu.partner.controller.v1

import com.coffee.gu.partner.auth.PartnerAuthenticated
import com.coffee.gu.partner.auth.PartnerPrincipal
import com.coffee.gu.partner.controller.v1.request.UpdateIngredientAvailabilityRequest
import com.coffee.gu.partner.controller.v1.response.PartnerIngredientResponse
import com.coffee.gu.partner.menu.PartnerIngredientService
import com.coffee.gu.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/partner/stores/{storeId}/ingredients")
class PartnerIngredientController(
    private val partnerIngredientService: PartnerIngredientService,
) {
    @GetMapping
    fun getStoreIngredients(
        @PartnerAuthenticated partnerPrincipal: PartnerPrincipal,
        @PathVariable storeId: Long,
    ): ApiResponse<List<PartnerIngredientResponse>> {
        val ingredients = partnerIngredientService.getStoreIngredients(partnerPrincipal, storeId)
        return ApiResponse.success(ingredients.map { PartnerIngredientResponse.from(it) })
    }

    @PatchMapping("/{ingredientId}/availability")
    fun updateAvailability(
        @PartnerAuthenticated partnerPrincipal: PartnerPrincipal,
        @PathVariable storeId: Long,
        @PathVariable ingredientId: Long,
        @RequestBody request: UpdateIngredientAvailabilityRequest,
    ): ApiResponse<PartnerIngredientResponse> {
        val updated = partnerIngredientService.updateAvailability(
            partnerPrincipal = partnerPrincipal,
            storeId = storeId,
            ingredientId = ingredientId,
            isAvailable = request.isAvailable,
        )
        return ApiResponse.success(PartnerIngredientResponse.from(updated))
    }
}
