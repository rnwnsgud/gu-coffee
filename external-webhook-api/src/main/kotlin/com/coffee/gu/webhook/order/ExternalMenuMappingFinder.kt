package com.coffee.gu.webhook.order

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.enums.DeliveryPlatform
import com.coffee.gu.webhook.ExternalMenuMappingRepository
import org.springframework.stereotype.Component

@Component
class ExternalMenuMappingFinder(
    private val externalMenuMappingRepository: ExternalMenuMappingRepository,
) {
    fun resolveMenuIds(
        platform: DeliveryPlatform,
        externalMenuCodes: Collection<String>,
    ): Map<String, Long> {
        val distinctCodes = externalMenuCodes.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        if (distinctCodes.isEmpty()) {
            throw CoreException(ErrorType.INVALID_REQUEST, "외부 메뉴 코드가 비어 있습니다.")
        }

        val mappings = externalMenuMappingRepository.findAllByPlatformAndExternalMenuCodeIn(
            platform = platform,
            externalMenuCodes = distinctCodes,
        ).associate { it.externalMenuCode to it.menuId }

        if (mappings.size != distinctCodes.size) {
            throw CoreException(ErrorType.MENU_MISMATCH_IN_ORDER, "매핑되지 않은 외부 플랫폼 메뉴 코드가 포함되어 있습니다.")
        }

        return mappings
    }
}
