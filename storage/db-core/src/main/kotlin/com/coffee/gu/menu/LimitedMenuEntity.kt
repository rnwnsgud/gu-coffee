package com.coffee.gu.menu

import com.coffee.gu.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Table(
    name = "limited_menu",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_limited_menu_menu_id",
            columnNames = ["menuId"],
        ),
    ],
)
@Entity
class LimitedMenuEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val menuId: Long,
    val totalQuantity: Int,
    var soldQuantity: Int = 0,
    val maxPurchasePerUser: Int = 1,
) : BaseEntity() {

    fun toModel(menu: Menu): LimitedMenu {
        return LimitedMenu(
            id = id,
            menu = menu,
            totalQuantity = totalQuantity,
            soldQuantity = soldQuantity,
            maxPurchasePerUser = maxPurchasePerUser,
        )
    }

    fun toPolicy(): LimitedMenuPolicy {
        return LimitedMenuPolicy(
            menuId = menuId,
            maxPurchasePerUser = maxPurchasePerUser,
        )
    }

    companion object {
        fun from(limitedMenu: LimitedMenu): LimitedMenuEntity {
            return LimitedMenuEntity(
                id = limitedMenu.id,
                menuId = limitedMenu.menu.id,
                totalQuantity = limitedMenu.totalQuantity,
                soldQuantity = limitedMenu.soldQuantity,
                maxPurchasePerUser = limitedMenu.maxPurchasePerUser,
            )
        }
    }
}
