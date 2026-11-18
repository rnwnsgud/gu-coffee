package com.coffee.gu.webhook

import com.coffee.gu.enums.DeliveryPlatform
import org.springframework.data.jpa.repository.JpaRepository

interface ExternalMenuMappingJpaRepository : JpaRepository<ExternalMenuMappingEntity, Long> {
    fun findAllByPlatformAndExternalMenuCodeIn(
        platform: DeliveryPlatform,
        externalMenuCodes: Collection<String>,
    ): List<ExternalMenuMappingEntity>
}
