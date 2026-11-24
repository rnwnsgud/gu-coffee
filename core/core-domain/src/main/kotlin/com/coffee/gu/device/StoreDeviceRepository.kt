package com.coffee.gu.device

interface StoreDeviceRepository {
    fun findByApiKeyHash(apiKeyHash: String): StoreDevice?
    fun save(device: StoreDevice): StoreDevice
}
