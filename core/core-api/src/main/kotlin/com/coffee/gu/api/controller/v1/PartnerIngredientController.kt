package com.coffee.gu.api.controller.v1

import com.coffee.gu.api.controller.v1.request.UpdateIngredientAvailabilityRequest
import com.coffee.gu.api.controller.v1.request.UpdateIngredientStockRequest
import com.coffee.gu.api.controller.v1.response.menu.PartnerIngredientResponse
import com.coffee.gu.menu.PartnerIngredientService
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
        @PathVariable storeId: Long,
    ): ApiResponse<List<PartnerIngredientResponse>> {
        val ingredients = partnerIngredientService.getStoreIngredients(storeId)
        return ApiResponse.success(ingredients.map { PartnerIngredientResponse.from(it) })
    }

    @PatchMapping("/{ingredientId}/availability")
    fun updateAvailability(
        @PathVariable storeId: Long,
        @PathVariable ingredientId: Long,
        @RequestBody request: UpdateIngredientAvailabilityRequest,
    ): ApiResponse<PartnerIngredientResponse> {
        val updated = partnerIngredientService.updateAvailability(
            storeId = storeId,
            ingredientId = ingredientId,
            isAvailable = request.isAvailable,
        )
        return ApiResponse.success(PartnerIngredientResponse.from(updated))
    }

    @PatchMapping("/{ingredientId}/stock")
    fun updateStockQuantity(
        @PathVariable storeId: Long,
        @PathVariable ingredientId: Long,
        @RequestBody request: UpdateIngredientStockRequest,
    ): ApiResponse<PartnerIngredientResponse> {
        val updated = partnerIngredientService.updateStockQuantity(
            storeId = storeId,
            ingredientId = ingredientId,
            stockQuantity = request.stockQuantity,
        )
        return ApiResponse.success(PartnerIngredientResponse.from(updated))
    }
}
