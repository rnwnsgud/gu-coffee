package com.coffee.gu.device

import com.coffee.gu.enums.StoreDeviceType

data class StoreDevice(
    val id: Long = 0,
    val deviceCode: String,
    val storeId: Long,
    val deviceType: StoreDeviceType,
    val apiKeyHash: String,
    val isActive: Boolean = true,
)
