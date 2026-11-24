package com.coffee.gu.device

import com.coffee.gu.BaseEntity
import com.coffee.gu.enums.StoreDeviceType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Table(name = "store_device")
@Entity
class StoreDeviceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    @Column(name = "device_code", nullable = false, length = 100)
    val deviceCode: String,
    @Column(name = "store_id", nullable = false)
    val storeId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 30)
    val deviceType: StoreDeviceType,
    @Column(name = "api_key_hash", nullable = false, length = 64)
    val apiKeyHash: String,
    @Column(name = "is_active", nullable = false)
    val enabled: Boolean = true,
) : BaseEntity() {

    fun toModel(): StoreDevice {
        return StoreDevice(
            id = id,
            deviceCode = deviceCode,
            storeId = storeId,
            deviceType = deviceType,
            apiKeyHash = apiKeyHash,
            isActive = enabled && isActive,
        )
    }

    companion object {
        fun from(device: StoreDevice): StoreDeviceEntity {
            return StoreDeviceEntity(
                id = device.id,
                deviceCode = device.deviceCode,
                storeId = device.storeId,
                deviceType = device.deviceType,
                apiKeyHash = device.apiKeyHash,
                enabled = device.isActive,
            )
        }
    }
}
