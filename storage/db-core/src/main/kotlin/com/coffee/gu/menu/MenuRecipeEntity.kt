package com.coffee.gu.menu

import com.coffee.gu.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Table(name = "menu_recipe")
@Entity
class MenuRecipeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val menuId: Long,
    val ingredientId: Long,
    var requiredQuantity: Int = 1,
) : BaseEntity() {

    fun toModel(): MenuRecipe {
        return MenuRecipe(
            id = id,
            menuId = menuId,
            ingredientId = ingredientId,
            requiredQuantity = requiredQuantity,
        )
    }

    companion object {
        fun from(menuRecipe: MenuRecipe): MenuRecipeEntity {
            return MenuRecipeEntity(
                id = menuRecipe.id,
                menuId = menuRecipe.menuId,
                ingredientId = menuRecipe.ingredientId,
                requiredQuantity = menuRecipe.requiredQuantity,
            )
        }
    }
}
