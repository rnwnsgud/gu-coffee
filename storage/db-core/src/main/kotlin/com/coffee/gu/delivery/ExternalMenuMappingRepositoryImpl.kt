package com.coffee.gu.delivery

import com.coffee.gu.enums.DeliveryPlatform
import org.springframework.stereotype.Repository

@Repository
class ExternalMenuMappingRepositoryImpl(
    private val externalMenuMappingJpaRepository: ExternalMenuMappingJpaRepository,
) : ExternalMenuMappingRepository {

    override fun findAllByPlatformAndExternalMenuCodeIn(
        platform: DeliveryPlatform,
        externalMenuCodes: Collection<String>,
    ): List<ExternalMenuMapping> {
        if (externalMenuCodes.isEmpty()) return emptyList()
        return externalMenuMappingJpaRepository
            .findAllByPlatformAndExternalMenuCodeIn(platform, externalMenuCodes)
            .map { it.toModel() }
    }

    override fun saveAll(mappings: Collection<ExternalMenuMapping>): List<ExternalMenuMapping> {
        if (mappings.isEmpty()) return emptyList()
        val savedMappings = externalMenuMappingJpaRepository.saveAll(
            mappings.map { ExternalMenuMappingEntity.from(it) },
        )
        return savedMappings.map { it.toModel() }
    }
}
