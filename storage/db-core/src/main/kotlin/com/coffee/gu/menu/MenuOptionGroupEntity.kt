package com.coffee.gu.menu

import com.coffee.gu.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Table(name = "menu_option_group")
@Entity
class MenuOptionGroupEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val menuId: Long,
    val optionGroupId: Long,
) : BaseEntity() {

    fun toModel(): MenuOptionGroup {
        return MenuOptionGroup(
            menuId = menuId,
            optionGroupId = optionGroupId,
        )
    }
}
