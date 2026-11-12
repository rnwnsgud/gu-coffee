package com.coffee.gu.menu

import com.coffee.gu.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Table(name = "ingredient")
@Entity
class IngredientEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val storeId: Long,
    val ingredientId: Long,
    val name: String,
    var stockQuantity: Int = 0,
    var isAvailable: Boolean = true,
) : BaseEntity() {

    fun toModel(): Ingredient {
        return Ingredient(
            id = id,
            storeId = storeId,
            ingredientId = ingredientId,
            name = name,
            stockQuantity = stockQuantity,
            isAvailable = isAvailable,
        )
    }

    companion object {
        fun from(ingredient: Ingredient): IngredientEntity {
            return IngredientEntity(
                id = ingredient.id,
                storeId = ingredient.storeId,
                ingredientId = ingredient.ingredientId,
                name = ingredient.name,
                stockQuantity = ingredient.stockQuantity,
                isAvailable = ingredient.isAvailable,
            )
        }
    }
}
