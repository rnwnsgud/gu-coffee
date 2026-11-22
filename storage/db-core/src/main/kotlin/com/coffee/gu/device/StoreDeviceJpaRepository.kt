package com.coffee.gu.device

import org.springframework.data.jpa.repository.JpaRepository

interface StoreDeviceJpaRepository : JpaRepository<StoreDeviceEntity, Long> {
    fun findByApiKeyHash(apiKeyHash: String): StoreDeviceEntity?
}
