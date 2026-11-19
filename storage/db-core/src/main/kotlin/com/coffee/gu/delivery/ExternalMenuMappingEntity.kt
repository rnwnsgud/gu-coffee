package com.coffee.gu.delivery

import com.coffee.gu.BaseEntity
import com.coffee.gu.enums.DeliveryPlatform
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Table(name = "external_menu_mapping")
@Entity
class ExternalMenuMappingEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    @Enumerated(EnumType.STRING)
    val platform: DeliveryPlatform,
    val externalMenuCode: String,
    val menuId: Long,
) : BaseEntity() {

    fun toModel(): ExternalMenuMapping {
        return ExternalMenuMapping(
            id = id,
            platform = platform,
            externalMenuCode = externalMenuCode,
            menuId = menuId,
        )
    }

    companion object {
        fun from(mapping: ExternalMenuMapping): ExternalMenuMappingEntity {
            return ExternalMenuMappingEntity(
                id = mapping.id,
                platform = mapping.platform,
                externalMenuCode = mapping.externalMenuCode,
                menuId = mapping.menuId,
            )
        }
    }
}
