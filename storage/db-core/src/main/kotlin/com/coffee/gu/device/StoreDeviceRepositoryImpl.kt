package com.coffee.gu.device

import org.springframework.stereotype.Repository

@Repository
class StoreDeviceRepositoryImpl(
    private val storeDeviceJpaRepository: StoreDeviceJpaRepository,
) : StoreDeviceRepository {

    override fun findByApiKeyHash(apiKeyHash: String): StoreDevice? {
        return storeDeviceJpaRepository.findByApiKeyHash(apiKeyHash)?.toModel()
    }

    override fun save(device: StoreDevice): StoreDevice {
        return storeDeviceJpaRepository.save(StoreDeviceEntity.from(device)).toModel()
    }
}
