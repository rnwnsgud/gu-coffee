package com.coffee.gu.menu

import com.coffee.gu.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Table(name = "menu_category")
@Entity
class MenuCategoryEntity @JvmOverloads constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val menuId: Long,
    val categoryId: Long,
) : BaseEntity() {

    fun toModel(): MenuCategory {
        return MenuCategory(id, menuId, categoryId)
    }
}
