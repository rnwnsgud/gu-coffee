package com.coffee.gu.api.controller.v1.response.menu

import com.coffee.gu.menu.MenuDetailResult
import com.coffee.gu.menu.Option
import com.coffee.gu.menu.OptionGroup
import java.math.BigDecimal

class MenuDetailResponse(
    val id: Long,
    val name: String,
    val salesPrice: BigDecimal,
    val imageUrl: String?,
    val description: String?,
    val capacity: Double?,
    val caffeine: Double?,
    val calories: Double?,
    val sodium: Double?,
    val carbohydrate: Double?,
    val sugar: Double?,
    val fat: Double?,
    val saturatedFat: Double?,
    val protein: Double?,
    val containedAllergens: String?,
    val mayContainAllergens: String?,
    val optionGroups: List<OptionGroupResponse>
) {
    companion object {
        fun from(menuDetail: MenuDetailResult): MenuDetailResponse {
            val menu = menuDetail.menu
            val detail = menu.detail
            val nutrition = detail?.nutrition

            return MenuDetailResponse(
                id = menu.id,
                name = menu.name,
                salesPrice = menu.salesPrice,
                imageUrl = menu.imageUrl,
                description = menu.description,
                capacity = nutrition?.capacity,
                caffeine = nutrition?.caffeine,
                calories = nutrition?.calories,
                sodium = nutrition?.sodium,
                carbohydrate = nutrition?.carbohydrate,
                sugar = nutrition?.sugar,
                fat = nutrition?.fat,
                saturatedFat = nutrition?.saturatedFat,
                protein = nutrition?.protein,
                containedAllergens = detail?.containedAllergens,
                mayContainAllergens = detail?.mayContainAllergens,
                optionGroups = OptionGroupResponse.of(menuDetail.optionGroups, menuDetail.options)
            )
        }
    }

    class OptionGroupResponse(
        val id: Long,
        val name: String,
        val isExclusive: Boolean,
        val isRequired: Boolean,
        val options: List<OptionResponse>,
    ) {
        companion object {
            fun of(optionGroups: List<OptionGroup>, options: List<Option>): List<OptionGroupResponse> {
                val optionsByGroup = options.groupBy { it.optionGroupId }
                return optionGroups.map { optionGroup ->
                    OptionGroupResponse(
                        id = optionGroup.id,
                        name = optionGroup.name,
                        isExclusive = optionGroup.isExclusive,
                        isRequired = optionGroup.isRequired,
                        options = OptionResponse.from(optionsByGroup[optionGroup.id] ?: emptyList())
                    )
                }
            }
        }
    }

    class OptionResponse(
        val id: Long,
        val name: String,
        val extraPrice: BigDecimal
    ) {
        companion object {
            fun from(options: List<Option>): List<OptionResponse> {
                return options.map { option ->
                    OptionResponse(
                        id = option.id,
                        name = option.name,
                        extraPrice = option.extraPrice
                    )
                }
            }
        }
    }
}
