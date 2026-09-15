package com.coffee.gu.menu

import com.coffee.gu.BaseEntity
import com.coffee.gu.enums.MenuType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Table(name = "menu")
@Entity
class MenuEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val name: String,
    @Enumerated(EnumType.STRING)
    val type: MenuType,
    val costPrice: BigDecimal,
    val salesPrice: BigDecimal,
    val imageUrl: String? = null,
    val description: String? = null,
    val capacity: Double? = null,
    val caffeine: Double? = null,
    val calories: Double? = null,
    val sodium: Double? = null,
    val carbohydrate: Double? = null,
    val sugar: Double? = null,
    val fat: Double? = null,
    val saturatedFat: Double? = null,
    val protein: Double? = null,
    val containedAllergens: String? = null,
    val mayContainAllergens: String? = null,
) : BaseEntity() {

    fun toModel(): Menu {
        return Menu(
            id = id,
            name = name,
            type = type,
            price = Price(costPrice, salesPrice),
            imageUrl = imageUrl,
            description = description,
            detail = MenuDetail(
                nutrition = Nutrition(
                    capacity = capacity,
                    caffeine = caffeine,
                    calories = calories,
                    sodium = sodium,
                    carbohydrate = carbohydrate,
                    sugar = sugar,
                    fat = fat,
                    saturatedFat = saturatedFat,
                    protein = protein,
                ),
                containedAllergens = containedAllergens,
                mayContainAllergens = mayContainAllergens,
            ),
        )
    }

    companion object {
        fun from(menu: Menu): MenuEntity = MenuEntity(
            id = menu.id,
            name = menu.name ?: "",
            type = menu.type ?: MenuType.DRINK,
            costPrice = menu.price?.costPrice ?: BigDecimal.ZERO,
            salesPrice = menu.price?.salesPrice ?: BigDecimal.ZERO,
            imageUrl = menu.imageUrl,
            description = menu.description,
            capacity = menu.detail?.nutrition?.capacity,
            caffeine = menu.detail?.nutrition?.caffeine,
            calories = menu.detail?.nutrition?.calories,
            sodium = menu.detail?.nutrition?.sodium,
            carbohydrate = menu.detail?.nutrition?.carbohydrate,
            sugar = menu.detail?.nutrition?.sugar,
            fat = menu.detail?.nutrition?.fat,
            saturatedFat = menu.detail?.nutrition?.saturatedFat,
            protein = menu.detail?.nutrition?.protein,
            containedAllergens = menu.detail?.containedAllergens,
            mayContainAllergens = menu.detail?.mayContainAllergens,
        )
    }
}
