package com.coffee.gu.webhook

import com.coffee.gu.enums.DeliveryPlatform

interface ExternalMenuMappingRepository {
    fun findAllByPlatformAndExternalMenuCodeIn(
        platform: DeliveryPlatform,
        externalMenuCodes: Collection<String>,
    ): List<ExternalMenuMapping>

    fun saveAll(mappings: Collection<ExternalMenuMapping>): List<ExternalMenuMapping>
}
